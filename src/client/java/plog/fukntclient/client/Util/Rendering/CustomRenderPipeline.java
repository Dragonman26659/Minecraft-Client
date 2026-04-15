package plog.fukntclient.client.Util.Rendering;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.Getter;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import plog.fukntclient.FukntClient;

public class CustomRenderPipeline implements ClientModInitializer {

    // ── Singleton ─────────────────────────────────────────────────────────────

    @Getter
    private static CustomRenderPipeline instance;

    // ── Pipelines ─────────────────────────────────────────────────────────────

    /** QUADS — filled boxes and base quads */
    private static final RenderPipeline PIPELINE_FILLED = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(ResourceLocation.fromNamespaceAndPath(FukntClient.MOD_ID, "pipeline/filled_through_walls"))
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .build()
    );

    /** LINES — wireframe edges and tracers */
    private static final RenderPipeline PIPELINE_LINES = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(ResourceLocation.fromNamespaceAndPath(FukntClient.MOD_ID, "pipeline/lines_through_walls"))
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .build()
    );

    // ── Allocators (one per pipeline) ─────────────────────────────────────────

    private static final ByteBufferBuilder allocatorFilled = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
    private static final ByteBufferBuilder allocatorLines  = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);

    // ── GPU uniforms ──────────────────────────────────────────────────────────

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET    = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX  = new Matrix4f();

    // ── GPU vertex ring buffers ───────────────────────────────────────────────

    private MappableRingBuffer vbFilled;
    private MappableRingBuffer vbLines;

    // ── Per-frame BufferBuilders ──────────────────────────────────────────────

    private BufferBuilder bufFilled;
    private BufferBuilder bufLines;

    // ── Queued draw commands (recorded before the render event fires) ──────────

    private record FilledBoxCmd(float x0, float y0, float z0,
                                float x1, float y1, float z1,
                                float r,  float g,  float b, float a) {}

    private record BaseFilledCmd(float x0, float y0, float z0,
                                 float r,  float g,  float b, float a) {}

    private record WireFrameCmd(float x0, float y0, float z0,
                                float x1, float y1, float z1,
                                float r,  float g,  float b, float a) {}

    private record TracerCmd(float tx, float ty, float tz,
                             float r,  float g,  float b, float a) {}

    private final List<FilledBoxCmd>  filledQueue  = new ArrayList<>();
    private final List<BaseFilledCmd> baseQueue    = new ArrayList<>();
    private final List<WireFrameCmd>  wireQueue    = new ArrayList<>();
    private final List<TracerCmd>     tracerQueue  = new ArrayList<>();

    // ── Public queue API (called by Render.java) ──────────────────────────────

    public void queueFilledBox(float x0, float y0, float z0,
                               float x1, float y1, float z1,
                               float r, float g, float b, float a) {
        filledQueue.add(new FilledBoxCmd(x0, y0, z0, x1, y1, z1, r, g, b, a));
    }

    public void queueBaseFilled(float x0, float y0, float z0,
                                float r, float g, float b, float a) {
        baseQueue.add(new BaseFilledCmd(x0, y0, z0, r, g, b, a));
    }

    public void queueWireFrame(float x0, float y0, float z0,
                               float x1, float y1, float z1,
                               float r, float g, float b, float a) {
        wireQueue.add(new WireFrameCmd(x0, y0, z0, x1, y1, z1, r, g, b, a));
    }

    public void queueTracer(float tx, float ty, float tz,
                            float r, float g, float b, float a) {
        tracerQueue.add(new TracerCmd(tx, ty, tz, r, g, b, a));
    }

    // ── ClientModInitializer ──────────────────────────────────────────────────

    @Override
    public void onInitializeClient() {
        instance = this;
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(this::onRender);
    }

    // ── Render event ─────────────────────────────────────────────────────────

    private void onRender(WorldRenderContext context) {
        if (filledQueue.isEmpty() && baseQueue.isEmpty()
                && wireQueue.isEmpty() && tracerQueue.isEmpty()) return;

        PoseStack matrices = context.matrices();
        Vec3 camera = context.worldState().cameraRenderState.pos;

        // ── Extraction: write all queued geometry into the BufferBuilders ──────

        // Filled (QUADS)
        if (!filledQueue.isEmpty() || !baseQueue.isEmpty()) {
            bufFilled = new BufferBuilder(allocatorFilled,
                    PIPELINE_FILLED.getVertexFormatMode(),
                    PIPELINE_FILLED.getVertexFormat());

            matrices.pushPose();
            matrices.translate(-camera.x, -camera.y, -camera.z);
            Matrix4f mat = matrices.last().pose();

            for (FilledBoxCmd cmd : filledQueue) {
                writeFilledBox(mat, bufFilled,
                        cmd.x0(), cmd.y0(), cmd.z0(),
                        cmd.x1(), cmd.y1(), cmd.z1(),
                        cmd.r(), cmd.g(), cmd.b(), cmd.a());
            }

            for (BaseFilledCmd cmd : baseQueue) {
                writeBaseFilled(mat, bufFilled,
                        cmd.x0(), cmd.y0(), cmd.z0(),
                        cmd.r(), cmd.g(), cmd.b(), cmd.a());
            }

            matrices.popPose();
            filledQueue.clear();
            baseQueue.clear();
        }

        // Lines (DEBUG_LINES)
        if (!wireQueue.isEmpty() || !tracerQueue.isEmpty()) {
            bufLines = new BufferBuilder(allocatorLines,
                    PIPELINE_LINES.getVertexFormatMode(),
                    PIPELINE_LINES.getVertexFormat());

            matrices.pushPose();
            matrices.translate(-camera.x, -camera.y, -camera.z);
            Matrix4f mat = matrices.last().pose();

            for (WireFrameCmd cmd : wireQueue) {
                writeWireFrame(mat, bufLines,
                        cmd.x0(), cmd.y0(), cmd.z0(),
                        cmd.x1(), cmd.y1(), cmd.z1(),
                        cmd.r(), cmd.g(), cmd.b(), cmd.a());
            }

            for (TracerCmd cmd : tracerQueue) {
                float cx = (float) camera.x;
                float cy = (float) camera.y;
                float cz = (float) camera.z;

                // Calculate tracer directional vector
                float nx = cmd.tx() - cx;
                float ny = cmd.ty() - cy;
                float nz = cmd.tz() - cz;
                float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);

                if (len != 0) {
                    nx /= len; ny /= len; nz /= len;
                } else {
                    nx = 1f; ny = 0f; nz = 0f;
                }

                // Tracer: camera pos → target
                bufLines.addVertex(mat, cx, cy, cz)
                        .setColor(cmd.r(), cmd.g(), cmd.b(), cmd.a())
                        .setNormal(nx, ny, nz);

                bufLines.addVertex(mat, cmd.tx(), cmd.ty(), cmd.tz())
                        .setColor(cmd.r(), cmd.g(), cmd.b(), cmd.a())
                        .setNormal(nx, ny, nz);
            }

            matrices.popPose();
            wireQueue.clear();
            tracerQueue.clear();
        }

        // ── Drawing: flush both buffers to the GPU ─────────────────────────────

        if (bufFilled != null) {
            vbFilled = flushBuffer(Minecraft.getInstance(), PIPELINE_FILLED,
                    bufFilled, allocatorFilled, vbFilled);
            bufFilled = null;
        }

        if (bufLines != null) {
            vbLines = flushBuffer(Minecraft.getInstance(), PIPELINE_LINES,
                    bufLines, allocatorLines, vbLines);
            bufLines = null;
        }
    }

    // ── Geometry writers ──────────────────────────────────────────────────────

    private static void writeFilledBox(Matrix4f mat, BufferBuilder buf,
                                       float x0, float y0, float z0,
                                       float x1, float y1, float z1,
                                       float r, float g, float b, float a) {
        // Front
        buf.addVertex(mat,x0,y0,z1).setColor(r,g,b,a); buf.addVertex(mat,x1,y0,z1).setColor(r,g,b,a);
        buf.addVertex(mat,x1,y1,z1).setColor(r,g,b,a); buf.addVertex(mat,x0,y1,z1).setColor(r,g,b,a);
        // Back
        buf.addVertex(mat,x1,y0,z0).setColor(r,g,b,a); buf.addVertex(mat,x0,y0,z0).setColor(r,g,b,a);
        buf.addVertex(mat,x0,y1,z0).setColor(r,g,b,a); buf.addVertex(mat,x1,y1,z0).setColor(r,g,b,a);
        // Left
        buf.addVertex(mat,x0,y0,z0).setColor(r,g,b,a); buf.addVertex(mat,x0,y0,z1).setColor(r,g,b,a);
        buf.addVertex(mat,x0,y1,z1).setColor(r,g,b,a); buf.addVertex(mat,x0,y1,z0).setColor(r,g,b,a);
        // Right
        buf.addVertex(mat,x1,y0,z1).setColor(r,g,b,a); buf.addVertex(mat,x1,y0,z0).setColor(r,g,b,a);
        buf.addVertex(mat,x1,y1,z0).setColor(r,g,b,a); buf.addVertex(mat,x1,y1,z1).setColor(r,g,b,a);
        // Top
        buf.addVertex(mat,x0,y1,z1).setColor(r,g,b,a); buf.addVertex(mat,x1,y1,z1).setColor(r,g,b,a);
        buf.addVertex(mat,x1,y1,z0).setColor(r,g,b,a); buf.addVertex(mat,x0,y1,z0).setColor(r,g,b,a);
        // Bottom
        buf.addVertex(mat,x0,y0,z0).setColor(r,g,b,a); buf.addVertex(mat,x1,y0,z0).setColor(r,g,b,a);
        buf.addVertex(mat,x1,y0,z1).setColor(r,g,b,a); buf.addVertex(mat,x0,y0,z1).setColor(r,g,b,a);
    }

    private static void writeBaseFilled(Matrix4f mat, BufferBuilder buf,
                                        float x0, float y0, float z0,
                                        float r, float g, float b, float a) {
        float x1 = x0 + 1f, z1 = z0 + 1f;
        // Tiny offset (0.005) prevents the quad from "sinking" into the block below
        float y = y0 + 0.005f;

        // Corrected winding order for a top-facing quad
        buf.addVertex(mat, x0, y, z1).setColor(r, g, b, a);
        buf.addVertex(mat, x1, y, z1).setColor(r, g, b, a);
        buf.addVertex(mat, x1, y, z0).setColor(r, g, b, a);
        buf.addVertex(mat, x0, y, z0).setColor(r, g, b, a);
    }

    private static void writeWireFrame(Matrix4f mat, BufferBuilder buf,
                                       float x0, float y0, float z0,
                                       float x1, float y1, float z1,
                                       float r, float g, float b, float a) {
        // Bottom ring
        edge(buf,mat, x0,y0,z0, x1,y0,z0, r,g,b,a); edge(buf,mat, x1,y0,z0, x1,y0,z1, r,g,b,a);
        edge(buf,mat, x1,y0,z1, x0,y0,z1, r,g,b,a); edge(buf,mat, x0,y0,z1, x0,y0,z0, r,g,b,a);
        // Top ring
        edge(buf,mat, x0,y1,z0, x1,y1,z0, r,g,b,a); edge(buf,mat, x1,y1,z0, x1,y1,z1, r,g,b,a);
        edge(buf,mat, x1,y1,z1, x0,y1,z1, r,g,b,a); edge(buf,mat, x0,y1,z1, x0,y1,z0, r,g,b,a);
        // Verticals
        edge(buf,mat, x0,y0,z0, x0,y1,z0, r,g,b,a); edge(buf,mat, x1,y0,z0, x1,y1,z0, r,g,b,a);
        edge(buf,mat, x1,y0,z1, x1,y1,z1, r,g,b,a); edge(buf,mat, x0,y0,z1, x0,y1,z1, r,g,b,a);
    }

    private static void edge(BufferBuilder buf, Matrix4f mat,
                             float ax, float ay, float az,
                             float bx, float by, float bz,
                             float r, float g, float b, float a) {

        // Calculate the directional vector for the normal
        float nx = bx - ax;
        float ny = by - ay;
        float nz = bz - az;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);

        // Normalize it, with a fallback to prevent division by zero
        if (len != 0) {
            nx /= len; ny /= len; nz /= len;
        } else {
            nx = 1f; ny = 0f; nz = 0f;
        }

        buf.addVertex(mat, ax, ay, az).setColor(r, g, b, a).setNormal(nx, ny, nz);
        buf.addVertex(mat, bx, by, bz).setColor(r, g, b, a).setNormal(nx, ny, nz);
    }

    // ── GPU upload + draw ─────────────────────────────────────────────────────

    private MappableRingBuffer flushBuffer(Minecraft client, RenderPipeline pipeline,
                                           BufferBuilder builder, ByteBufferBuilder allocator,
                                           MappableRingBuffer ringBuffer) {
        MeshData mesh = builder.buildOrThrow();
        MeshData.DrawState params = mesh.drawState();
        VertexFormat format = params.format();

        int vbSize = params.vertexCount() * format.getVertexSize();
        if (ringBuffer == null || ringBuffer.size() < vbSize) {
            if (ringBuffer != null) ringBuffer.close();
            ringBuffer = new MappableRingBuffer(
                    () -> FukntClient.MOD_ID + " render pipeline",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE,
                    vbSize
            );
        }

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        try (GpuBuffer.MappedView view = encoder.mapBuffer(
                ringBuffer.currentBuffer().slice(0, mesh.vertexBuffer().remaining()), false, true)) {
            MemoryUtil.memCopy(mesh.vertexBuffer(), view.data());
        }

        GpuBuffer vertices = ringBuffer.currentBuffer();
        GpuBuffer indices;
        VertexFormat.IndexType indexType;

        if (pipeline.getVertexFormatMode() == VertexFormat.Mode.QUADS) {
            mesh.sortQuads(allocator, RenderSystem.getProjectionType().vertexSorting());
            indices   = pipeline.getVertexFormat().uploadImmediateIndexBuffer(
                    Objects.requireNonNull(mesh.indexBuffer()));
            indexType = mesh.drawState().indexType();
        } else {
            RenderSystem.AutoStorageIndexBuffer seq =
                    RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
            indices   = seq.getBuffer(params.indexCount());
            indexType = seq.type();
        }

        GpuBufferSlice dynTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrix(),
                        COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX, 0.0f);

        try (RenderPass pass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                        () -> FukntClient.MOD_ID + " render pipeline rendering",
                        Objects.requireNonNull(client.getMainRenderTarget().getColorTextureView()),
                        OptionalInt.empty(),
                        client.getMainRenderTarget().getDepthTextureView(),
                        OptionalDouble.empty())) {

            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", dynTransforms);
            pass.setVertexBuffer(0, vertices);
            pass.setIndexBuffer(indices, indexType);
            pass.drawIndexed(0, 0, params.indexCount(), 1);
        }

        mesh.close();
        ringBuffer.rotate();
        return ringBuffer;
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    public void close() {
        allocatorFilled.close();
        allocatorLines.close();
        if (vbFilled != null) { vbFilled.close(); vbFilled = null; }
        if (vbLines  != null) { vbLines.close();  vbLines  = null; }
    }
}
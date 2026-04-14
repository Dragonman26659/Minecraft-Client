package plog.fukntclient.client.events;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EventBus {
    private final List<Consumer<PacketEvent.Send>>    sendListeners    = new ArrayList<>();
    private final List<Consumer<PacketEvent.Receive>> receiveListeners = new ArrayList<>();

    public void onSend(Consumer<PacketEvent.Send> listener)       { sendListeners.add(listener); }
    public void onReceive(Consumer<PacketEvent.Receive> listener) { receiveListeners.add(listener); }

    public PacketEvent.Send fireSend(PacketEvent.Send event) {
        for (Consumer<PacketEvent.Send> l : sendListeners) l.accept(event);
        return event;
    }

    public PacketEvent.Receive fireReceive(PacketEvent.Receive event) {
        for (Consumer<PacketEvent.Receive> l : receiveListeners) l.accept(event);
        return event;
    }
}
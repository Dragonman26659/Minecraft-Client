package plog.fukntclient.client.Managers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocationData {
    private int x;
    private int y;
    private int z;
    private String dimension;
}
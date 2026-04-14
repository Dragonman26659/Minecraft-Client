package plog.fukntclient.client.settings;


import lombok.Getter;
import lombok.Setter;

@Getter
public class Setting {
    @Setter
    public String name;
    public boolean isActive = true;
}

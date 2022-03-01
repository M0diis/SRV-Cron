package me.m0dii.srvcron.utils;

public enum EventType
{
    JOIN_EVENT("join-event"),
    QUIT_EVENT("quit-event"),
    WEATHER_CHANGE_EVENT("weather-change-event"),
    WORLD_LOAD_EVENT("world-load-event"),
    PLAYER_BED_ENTER_EVENT("player-bed-enter-event"),
    PLAYER_CHANGE_WORLD_EVENT("player-change-world-event");

    private final String configName;

    EventType(String configName)
    {
        this.configName = configName;
    }

    public String getConfigName()
    {
        return configName;
    }

    public static EventType isEventJob(String string)
    {
        for(EventType type : values())
        {
            if(type.getConfigName().equalsIgnoreCase(string))
                return type;
        }
        
        return null;
    }
}

package shilov.vadim;

import java.util.Collection;

public interface RoomCreator {

  void createRoom(long currentTime, Collection<Long> playerIds);

}

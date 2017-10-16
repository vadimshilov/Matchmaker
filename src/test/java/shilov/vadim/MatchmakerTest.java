package shilov.vadim;

import org.junit.Test;
import shilov.vadim.impl.ConsoleOutputRoomCreator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static shilov.vadim.util.TimeUtils.getCurrentTime;

public class MatchmakerTest {

  private long[] enterTime;
  private int[] rank;
  private Set<Long> assignedPlayers = ConcurrentHashMap.newKeySet();
  private volatile Throwable parallelThreadThrowable = null;

  private static final int TIME_COEFFICIENT = 5000;

  private static final int REQUIRED_ROOM_SIZE = 8;
  private static final int MAX_RANK = 30;

  @Test
  public void matchmakerTest() throws Throwable {
    Matchmaker matchmaker = new Matchmaker(new RoomValidator());

    int playerCount = 128;
    enterTime = new long[playerCount];
    rank = new int[playerCount];
    Random rand = new Random(0x474747);

    for (int i = 0; i < playerCount; i++) {
      int currentRank = rand.nextInt(MAX_RANK) + 1;
      rank[i] = currentRank;
      long id = i;
      long currentTime = getCurrentTime();
      enterTime[i] = currentTime;
      matchmaker.addPlayerToQueue(id, currentRank);


      int sleepTime = rand.nextInt(500);
//      System.err.printf("Added player with rank = %d, sleep for %d ms.\n", currentRank, sleepTime);
//      System.err.flush();

      Thread.sleep(sleepTime);
      if (parallelThreadThrowable != null) {
        throw parallelThreadThrowable;
      }
    }
    long startWaitTime = getCurrentTime();

    long waitTime = (MAX_RANK / 2) * TIME_COEFFICIENT;
    while (assignedPlayers.size() < playerCount) {
      long currentTime = getCurrentTime();
      assertTrue(currentTime - startWaitTime < waitTime);
      if (parallelThreadThrowable != null) {
        throw parallelThreadThrowable;
      }
    }
  }

  private class RoomValidator implements RoomCreator {

    // just to see results in console
//    private ConsoleOutputRoomCreator consoleOutputRoomCreator = new ConsoleOutputRoomCreator(System.err);

    @Override
    public void createRoom(long currentTime, Collection<Long> playerIds) {
      try {
        // room size
        assertEquals(REQUIRED_ROOM_SIZE, playerIds.size());

        // no players which are already assigned
        playerIds.forEach(playerId -> {
          assertTrue(playerId.toString() + " is already assigned", assignedPlayers.add(playerId));
        });

        // abs(A.rank - B.rank) <= waiting_time(A) / 5000 + waiting_time(B) / 5000
        List<Long> playerList = new ArrayList<>(playerIds);
        for (int i = 0; i < playerList.size(); i++) {
          for (int j = i + 1; j < playerList.size(); j++) {
            int id1 = (int) (long) playerList.get(i);
            int id2 = (int) (long) playerList.get(j);
            int rankDiff = Math.abs(rank[id1] - rank[id2]);
            long waitingTime1 = (currentTime - enterTime[id1]) / TIME_COEFFICIENT;
            long waitingTime2 = (currentTime - enterTime[id2]) / TIME_COEFFICIENT;
            assertTrue(rankDiff <= waitingTime1 + waitingTime2);
          }
        }
//        consoleOutputRoomCreator.createRoom(currentTime, playerIds);
      } catch (Throwable e) {
        parallelThreadThrowable = e;
      }
    }
  }
}

package shilov.vadim;

import shilov.vadim.pojo.UserInfo;

import java.util.*;
import java.util.concurrent.LinkedTransferQueue;

import static shilov.vadim.util.TimeUtils.getCurrentTime;

public class Matchmaker {

  private static final int MAX_RANK = 30;
  private static final int ROOM_SIZE = 8;
  private static final int TIME_COEFFICIENT = 5000;

  private LinkedTransferQueue<UserInfo> requestQueue = new LinkedTransferQueue<>();
  private RoomCreator roomCreator;

  public Matchmaker(RoomCreator roomCreator) {
    this.roomCreator = roomCreator;
    new MatchmakerThread().start();
  }

  public void addPlayerToQueue(long playerId, int rank) {
    // rank in UserInfo musst satisfy condition 0 <= rank < MAX_RANK
    requestQueue.put(new UserInfo(playerId, rank - 1, getCurrentTime()));
  }

  private class MatchmakerThread extends Thread {

    private Queue<UserInfo> userQueue = new LinkedList<>();
    private Set<Long>[] possibleChooses = new HashSet[MAX_RANK];
    private Map<Long, Integer> waitSteps = new HashMap<>();
    private Map<Long, UserInfo> userMap = new HashMap<>();

    public MatchmakerThread() {
      for (int i = 0; i < MAX_RANK; i++) {
        possibleChooses[i] = new HashSet<>();
      }
    }

    @Override
    public void run() {
      while (true) {
        if (interrupted()) {
          break;
        }
        processUserRequests();
        if (userQueue.isEmpty()) {
          try {
            sleep(1);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          continue;
        }
        UserInfo userInfo = userQueue.peek();
        Integer currentUserWaitSteps = waitSteps.get(userInfo.getId());
        if (currentUserWaitSteps == null) {
          userQueue.poll();
        } else {
          long currentMillis = getCurrentTime();
          // we do not need to consider wait steps more than MAX_RANK
          int newWaitSteps = (int) Math.min((currentMillis - userInfo.getEnterTime()) / TIME_COEFFICIENT, MAX_RANK);
          if (newWaitSteps > currentUserWaitSteps) {
            waitSteps.put(userInfo.getId(), newWaitSteps);
            userQueue.poll();
            int left = userInfo.getRank() - newWaitSteps;
            boolean leftGood = false;
            if (left >= 0) {
              possibleChooses[left].add(userInfo.getId());
              leftGood = possibleChooses[left].size() == ROOM_SIZE;
            }
            int right = userInfo.getRank() + newWaitSteps;
            boolean rightGood = false;
            if (right < MAX_RANK) {
              possibleChooses[right].add(userInfo.getId());
              rightGood = possibleChooses[right].size() == ROOM_SIZE;
            }
            if (leftGood && rightGood) {
              createRoom(possibleChooses[left], possibleChooses[right]);
            } else if (leftGood) {
              createRoom(possibleChooses[left]);
            } else if (rightGood) {
              createRoom(possibleChooses[right]);
            } else {
              userQueue.add(userInfo);
            }
          } else {
            try {
              sleep(1);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
        }
      }
    }

    private void processUserRequests() {
      List<UserInfo> newUserInfoList = new ArrayList<>();
      requestQueue.drainTo(newUserInfoList);
      newUserInfoList.forEach(newUserInfo -> {
        waitSteps.put(newUserInfo.getId(), 0);
        userMap.put(newUserInfo.getId(), newUserInfo);
        possibleChooses[newUserInfo.getRank()].add(newUserInfo.getId());
        if (possibleChooses[newUserInfo.getRank()].size() == ROOM_SIZE) {
          createRoom(possibleChooses[newUserInfo.getRank()]);
        } else {
          userQueue.add(newUserInfo);
        }
      });
    }

    private void createRoom(Set<Long> leftPlayerIds, Set<Long> rightPlayerIds) {
      long leftEnterTime = leftPlayerIds
          .stream()
          .map(userMap::get)
          .mapToLong(UserInfo::getEnterTime)
          .sum();
      long rightEnterTime = rightPlayerIds
          .stream()
          .map(userMap::get)
          .mapToLong(UserInfo::getEnterTime)
          .sum();
      if (leftEnterTime < rightEnterTime) {
        createRoom(leftPlayerIds);
      } else {
        createRoom(rightPlayerIds);
      }
    }

    private void createRoom(Set<Long> playerIds) {
      // playerIds is element of possibleChooses, it will become empty at one loop step, so we need need its copy
      Set<Long> playerIdsCopy = new HashSet<>(playerIds);
      roomCreator.createRoom(getCurrentTime(), playerIdsCopy);
      for (int i = 0; i < MAX_RANK; i++) {
        possibleChooses[i].removeAll(playerIdsCopy);
      }
      playerIdsCopy.forEach(playerId -> {
        waitSteps.remove(playerId);
        userMap.remove(playerId);
      });
    }
  }

}

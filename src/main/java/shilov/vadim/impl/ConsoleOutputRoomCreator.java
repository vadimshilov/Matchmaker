package shilov.vadim.impl;

import shilov.vadim.RoomCreator;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.stream.Collectors;

public class ConsoleOutputRoomCreator implements RoomCreator {

  private PrintStream output = System.out;

  public ConsoleOutputRoomCreator() {
  }

  public ConsoleOutputRoomCreator(PrintStream output) {
    this.output = output;
  }

  @Override
  public void createRoom(long currentTime, Collection<Long> playerIds) {
    String playerString = playerIds.stream()
        .map(Object::toString)
        .collect(Collectors.joining(" "));
    output.println(currentTime + " " + playerString);
  }
}

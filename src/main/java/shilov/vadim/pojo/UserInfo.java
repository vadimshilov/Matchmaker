package shilov.vadim.pojo;

public class UserInfo {

  private long id;
  private int rank;
  private long enterTime;

  public UserInfo(long id, int rank, long enterTime) {
    this.id = id;
    this.rank = rank;
    this.enterTime = enterTime;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public int getRank() {
    return rank;
  }

  public void setRank(int rank) {
    this.rank = rank;
  }

  public long getEnterTime() {
    return enterTime;
  }

  public void setEnterTime(long enterTime) {
    this.enterTime = enterTime;
  }
}

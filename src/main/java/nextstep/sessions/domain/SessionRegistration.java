package nextstep.sessions.domain;

import java.util.HashSet;
import java.util.Set;
import nextstep.users.domain.NsUser;

public class SessionRegistration {
  private int capacity;

  private SessionStatus status;

  private Students students;

  public SessionRegistration(int capacity) {
    this(capacity, SessionStatus.READY, new HashSet<>());
  }

  public SessionRegistration(int capacity, SessionStatus status, Set<Student> students) {
    this(capacity, status, new Students(students));
  }

  public SessionRegistration(int capacity, SessionStatus status, Students students) {
    this.capacity = capacity;
    this.status = status;
    this.students = students;
  }

  public void recruitStart() {
    this.status = SessionStatus.RECRUITING;
  }

  public void recruitEnd() {
    this.status = SessionStatus.END;
  }

  public void enrolment(Session session, NsUser user) {
    if (students.size() > capacity) {
      throw new IllegalStateException("수강인원이 초과되었습니다");
    }

    if (students.contains(session, user)) {
      throw new IllegalStateException("이미 수강신청한 사용자입니다");
    }

    SessionStatus.isRecruitingOrThrow(status);

    students.add(session, user);
  }

  public void validateInit() {
    if (capacity <= 0) {
      throw new IllegalArgumentException("수강인원 수는 1명 이상이어야 합니다");
    }
  }

  public int getCapacity() {
    return this.capacity;
  }

  public SessionStatus getStatus() {
    return this.status;
  }

  public Students getStudents() {
    return this.students;
  }

  @Override
  public String toString() {
    return "SessionRegistration{" +
        "capacity=" + capacity +
        ", status=" + status +
        ", students=" + students +
        '}';
  }
}

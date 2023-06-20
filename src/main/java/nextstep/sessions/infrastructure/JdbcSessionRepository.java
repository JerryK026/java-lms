package nextstep.sessions.infrastructure;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import nextstep.sessions.domain.Session;
import nextstep.sessions.domain.SessionBody;
import nextstep.sessions.domain.SessionDate;
import nextstep.sessions.domain.SessionProgressStatus;
import nextstep.sessions.domain.SessionRegistration;
import nextstep.sessions.domain.SessionRepository;
import nextstep.sessions.domain.SessionRecruitingStatus;
import nextstep.sessions.domain.Student;
import nextstep.sessions.domain.Students;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

@Repository("sessionRepository")
public class JdbcSessionRepository implements SessionRepository {

  private JdbcOperations jdbcTemplate;

  public JdbcSessionRepository(JdbcOperations jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public int save(Session session) {
    String sessionInsertSql = "insert into session (start_date_time, end_date_time, title, contents, cover_image, capacity, session_recruiting_status_id, session_progress_status_id) values (?, ?, ?, ?, ?, ?, ?, ?)";

    return jdbcTemplate.update(sessionInsertSql, session.getStartDate(), session.getEndDate(),
        session.getTitle(), session.getContents(), session.getCoverImage(), session.getCapacity(),
        session.getRecruitingStatus().getOrder(), session.getProgressStatus().getOrder());
  }

  @Override
  public Session findById(Long id) {
    SessionEntity sessionEntity = getSessionEntity(id);
    SessionRecruitingStatus recruitingStatus = SessionRecruitingStatus.from(sessionEntity.sessionRecruitingStatusId);
    SessionProgressStatus progressStatus = SessionProgressStatus.from(sessionEntity.sessionProgressStatusId);

    // Session이 가지는 Users를 찾아오는 쿼리를 작성한다
    Students students = hasStudent(id) ? getStudents(id) : new Students(new HashSet<>());

    return new Session(
        sessionEntity.id,
        new SessionDate(sessionEntity.startDateTime, sessionEntity.endDateTime),
        new SessionBody(sessionEntity.title, sessionEntity.contents, sessionEntity.coverImage),
        new SessionRegistration(sessionEntity.capacity, recruitingStatus, progressStatus, students)
    );
  }

  @Override
  public void update(Session session) {
    String sql = "update session set start_date_time = ?, end_date_time = ?, title = ?, contents = ?, cover_image = ?, capacity = ?, session_recruiting_status_id = ?, session_progress_status_id = ? where id = ?";
    jdbcTemplate.update(sql, session.getStartDate(), session.getEndDate(), session.getTitle(),
        session.getContents(), session.getCoverImage(), session.getCapacity(),
        session.getRecruitingStatus().getOrder(), session.getProgressStatus().getOrder(), session.getId());

    updateStudents(session.getStudents());
  }

  private void updateStudents(Set<Student> students) {
    String sql = "insert into student (session_id, user_id, created_at) values (?, ?, ?)";
    LocalDateTime now = LocalDateTime.now();

    students
        .stream().filter(student -> student.getId().equals(0L))
        .forEach(student -> jdbcTemplate.update(sql, student.getSessionId(), student.getNsUserId(), now));
  }

  private SessionEntity getSessionEntity(Long id) {
    String sql = "select id, start_date_time, end_date_time, title, contents, cover_image, capacity, session_recruiting_status_id, session_progress_status_id from session where id = ?";
    SessionEntity sessionEntity = jdbcTemplate.queryForObject(sql,
        (rs, rowNum) -> new SessionEntity(rs.getLong(1),
            toLocalDateTime(rs.getTimestamp(2)), toLocalDateTime(rs.getTimestamp(3)),
            rs.getString(4), rs.getString(5), rs.getBytes(6),
            rs.getInt(7), rs.getLong(8), rs.getLong(9)),
        id
    );

    return sessionEntity;
  }

  private Students getStudents(Long sessionId) {
    String sql = "select id, session_id, user_id, created_at, updated_at from student where session_id = ?";

    List<StudentEntity> studentEntities = jdbcTemplate.query(sql,
        (rs, rowNum) -> new StudentEntity(rs.getLong(1), rs.getLong(2), rs.getLong(3),
            toLocalDateTime(rs.getTimestamp(4)), toLocalDateTime(rs.getTimestamp(5))),
        sessionId
    );

    Set<Student> students = studentEntities.stream()
        .map(su -> new Student(su.id, su.sessionId, su.userId, su.createdAt, su.updatedAt))
        .collect(Collectors.toSet());

    return new Students(students);
  }

  private boolean hasStudent(Long sessionId) {
    String sql = "select count(*) from student where session_id = ?";
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, sessionId);

    return !count.equals(0);
  }

  private LocalDateTime toLocalDateTime(Timestamp timestamp) {
    if (timestamp == null) {
      return null;
    }

    return timestamp.toLocalDateTime();
  }

  class StudentEntity {

    private Long id;
    private Long sessionId;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    StudentEntity(Long id, Long sessionId, Long userId, LocalDateTime createdAt,
        LocalDateTime updatedAt) {
      this.id = id;
      this.sessionId = sessionId;
      this.userId = userId;
      this.createdAt = createdAt;
      this.updatedAt = updatedAt;
    }
  }

  class SessionEntity {

    private Long id;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String title;
    private String contents;
    private byte[] coverImage;
    private int capacity;
    private Long sessionRecruitingStatusId;
    private Long sessionProgressStatusId;

    public SessionEntity(Long id, LocalDateTime startDateTime, LocalDateTime endDateTime, String title,
        String contents, byte[] coverImage, int capacity, Long sessionRecruitingStatusId, Long sessionProgressStatusId) {
      this.id = id;
      this.startDateTime = startDateTime;
      this.endDateTime = endDateTime;
      this.title = title;
      this.contents = contents;
      this.coverImage = coverImage;
      this.capacity = capacity;
      this.sessionRecruitingStatusId = sessionRecruitingStatusId;
      this.sessionProgressStatusId = sessionProgressStatusId;
    }
  }
}

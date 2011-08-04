package com.intellij.tasks.pivotal;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.tasks.*;
import com.intellij.tasks.impl.BaseRepository;
import com.intellij.tasks.impl.BaseRepositoryImpl;
import com.intellij.tasks.impl.SimpleComment;
import com.intellij.util.NullableFunction;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.net.HTTPMethod;
import com.intellij.util.xmlb.annotations.Tag;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Dennis.Ushakov
 */
@Tag("PivotalTracker")
public class PivotalTrackerRepository extends BaseRepositoryImpl {
  private static final Logger LOG = Logger.getInstance("#com.intellij.tasks.pivotal.PivotalTrackerRepository");
  private static final String API_URL = "/services/v3";
  private static final Pattern DATE_PATTERN = Pattern.compile("(\\d\\d\\d\\d[/-]\\d\\d[/-]\\d\\d).*(\\d\\d:\\d\\d:\\d\\d).*");
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

  private Pattern myPattern;
  private String myProjectId;
  private String myAPIKey;
  private boolean myShouldFormatCommitMessage;
  private String myCommitMessageFormat = "[fixes #{id}]";

  //private boolean myTasksSupport = false;

  {
    if (StringUtil.isEmpty(getUrl())) {
      setUrl("http://www.pivotaltracker.com");
    }
  }

  /** for serialization */
  @SuppressWarnings({"UnusedDeclaration"})
  public PivotalTrackerRepository() {}

  public PivotalTrackerRepository(final PivotalTrackerRepositoryType type) {
    super(type);
  }

  private PivotalTrackerRepository(final PivotalTrackerRepository other) {
    super(other);
    setProjectId(other.myProjectId);
    setAPIKey(other.myAPIKey);
    setShouldFormatCommitMessage(other.myShouldFormatCommitMessage);
    setCommitMessageFormat(other.myCommitMessageFormat);
  }

  @Override
  public void testConnection() throws Exception {
    getIssues("", 10, 0);
  }

  @Override
  public boolean isConfigured() {
    return super.isConfigured() &&
           StringUtil.isNotEmpty(getProjectId()) &&
           StringUtil.isNotEmpty(getAPIKey());
  }

  @Override
  public Task[] getIssues(@Nullable final String query, final int max, final long since) throws Exception {
    @SuppressWarnings({"unchecked"}) List<Object> children = getStories(query, max);
    List<Task> taskList = ContainerUtil.mapNotNull(children, new NullableFunction<Object, Task>() {
      public Task fun(Object o) {
        return createIssue((Element)o);
      }
    });

    return taskList.toArray(new Task[taskList.size()]);
  }

  @SuppressWarnings({"unchecked"})
  private List<Object> getStories(final String query, final int max) throws Exception {
    String url = API_URL + "/projects/" + myProjectId + "/stories";
    url += "?filter=" + encodeUrl("state:started,unstarted,unscheduled,rejected");
    if (!StringUtil.isEmpty(query)) {
      url += encodeUrl(" \"" + query + '"');
    }
    if (max >= 0) {
      url += "&limit=" + encodeUrl(String.valueOf(max));
    }
    LOG.info("Getting all the stories with url: " + url);
    final HttpMethod method = doREST(url, HTTPMethod.GET);
    final InputStream stream = method.getResponseBodyAsStream();
    final Element element = new SAXBuilder(false).build(stream).getRootElement();

    if (!"stories".equals(element.getName())) {
      LOG.warn("Error fetching issues for: " + url + ", HTTP status code: " + method.getStatusCode());
      throw new Exception("Error fetching issues for: " + url + ", HTTP status code: " + method.getStatusCode() +
                          "\n" + element.getText());
    }

    return element.getChildren("story");
  }

  @Nullable
  private Task createIssue(final Element element) {
    final String id = element.getChildText("id");
    if (id == null) {
      return null;
    }
    final String summary = element.getChildText("name");
    if (summary == null) {
      return null;
    }
    final String type = element.getChildText("story_type");
    if (type == null) {
      return null;
    }
    final Comment[] comments = parseComments(element.getChild("notes"));
    final boolean isClosed = "accepted".equals(element.getChildText("state")) ||
                             "delivered".equals(element.getChildText("state")) ||
                             "finished".equals(element.getChildText("state"));
    final String description = element.getChildText("description");
    final Ref<Date> updated = new Ref<Date>();
    final Ref<Date> created = new Ref<Date>();
    try {
      updated.set(parseDate(element, "updated_at"));
      created.set(parseDate(element, "created_at"));
    } catch (ParseException e) {
      LOG.warn(e);
    }

    return new Task() {
      @Override
      public boolean isIssue() {
        return true;
      }

      @Override
      public String getIssueUrl() {
        final String id = getRealId(getId());
        return id != null ? getUrl() + "/story/show/" + id : null;
      }

      @NotNull
      @Override
      public String getId() {
        return myProjectId + "-" + id;
      }

      @NotNull
      @Override
      public String getSummary() {
        return summary;
      }

      public String getDescription() {
        return description;
      }

      @NotNull
      @Override
      public Comment[] getComments() {
        return comments;
      }

      @Override
      public Icon getIcon() {
        return IconLoader.getIcon(getCustomIcon(), LocalTask.class);
      }

      @NotNull
      @Override
      public TaskType getType() {
        return TaskType.OTHER;
      }

      @Override
      public Date getUpdated() {
        return updated.get();
      }

      @Override
      public Date getCreated() {
        return created.get();
      }

      @Override
      public boolean isClosed() {
        return isClosed;
      }

      @Override
      public TaskRepository getRepository() {
        return PivotalTrackerRepository.this;
      }

      @Override
      public String getPresentableName() {
        return getId() + ": " + getSummary();
      }

      @NotNull
      @Override
      public String getCustomIcon() {
        return "/icons/pivotal/" + type + ".png";
      }
    };
  }

  private static Comment[] parseComments(Element notes) {
    if (notes == null) return Comment.EMPTY_ARRAY;
    final List<Comment> result = new ArrayList<Comment>();
    //noinspection unchecked
    for (Element note : (List<Element>)notes.getChildren("note")) {
      final String text = note.getChildText("text");
      if (text == null) continue;
      final Ref<Date> date = new Ref<Date>();
      try {
        date.set(parseDate(note, "noted_at"));
      } catch (ParseException e) {
        LOG.warn(e);
      }
      final String author = note.getChildText("author");
      result.add(new SimpleComment(date.get(), author, text));
    }
    return result.toArray(new Comment[result.size()]);
  }

  @Nullable
  public static Date parseDate(final Element element, final String name) throws ParseException {
    final Matcher m = DATE_PATTERN.matcher(element.getChildText(name));
    if (m.find()) {
      return DATE_FORMAT.parse(m.group(1).replace('-', '/') + " " + m.group(2));
    }
    return null;
  }

  private HttpMethod doREST(final String request, final HTTPMethod type) throws Exception {
    final HttpClient client = getHttpClient();
    client.getParams().setContentCharset("UTF-8");
    final String uri = getUrl() + request;
    final HttpMethod method = type == HTTPMethod.POST ? new PostMethod(uri) :
                              type == HTTPMethod.PUT ? new PutMethod(uri) : new GetMethod(uri);
    configureHttpMethod(method);
    client.executeMethod(method);
    return method;
  }

  @Override
  public Task findTask(final String id) throws Exception {
    final String realId = getRealId(id);
    if (realId == null) return null;
    final String url = API_URL + "/projects/" + myProjectId + "/stories/" + realId;
    LOG.info("Retrieving issue by id: " + url);
    final HttpMethod method = doREST(url, HTTPMethod.GET);
    final InputStream stream = method.getResponseBodyAsStream();
    final Element element = new SAXBuilder(false).build(stream).getRootElement();
    return element.getName().equals("story") ? createIssue(element) : null;
  }

  @Nullable
  private String getRealId(final String id) {
    final String[] split = id.split("\\-");
    final String projectId = split[0];
    return Comparing.strEqual(projectId, myProjectId) ? split[1] : null;
  }

  @Nullable
  public String extractId(final String taskName) {
    Matcher matcher = myPattern.matcher(taskName);
    return matcher.find() ? matcher.group(1) : null;
  }

  @Override
  public BaseRepository clone() {
    return new PivotalTrackerRepository(this);
  }

  @Override
  protected void configureHttpMethod(final HttpMethod method) {
    method.addRequestHeader("X-TrackerToken", myAPIKey);
  }

  public String getProjectId() {
    return myProjectId;
  }
  
  public void setProjectId(final String projectId) {
    myProjectId = projectId;
    myPattern = Pattern.compile("(" + projectId + "\\-\\d+):\\s+");
  }

  public String getAPIKey() {
    return myAPIKey;
  }

  public void setAPIKey(final String APIKey) {
    myAPIKey = APIKey;
  }

  @Override
  public String getPresentableName() {
    final String name = super.getPresentableName();
    return name + (!StringUtil.isEmpty(getProjectId()) ? "/" + getProjectId() : "");
  }

  @Override
  public String getTaskComment(final Task task) {
    if (isShouldFormatCommitMessage()) {
      final String id = task.getId();
      final String realId = getRealId(id);
      return realId != null ?
             myCommitMessageFormat.replace("{id}", realId).replace("{project}", myProjectId) + " " + task.getSummary() :
             null;
    }
    return super.getTaskComment(task);
  }

  public boolean isShouldFormatCommitMessage() {
    return myShouldFormatCommitMessage;
  }

  public void setShouldFormatCommitMessage(final boolean shouldFormatCommitMessage) {
    myShouldFormatCommitMessage = shouldFormatCommitMessage;
  }

  @Tag("commitMessageFormat")
  public String getCommitMessageFormat() {
    return myCommitMessageFormat;
  }

  public void setCommitMessageFormat(final String commitMessageFormat) {
    myCommitMessageFormat = commitMessageFormat;
  }

  @Override
  public void setTaskState(Task task, TaskState state) throws Exception {
    if (state != TaskState.IN_PROGRESS) super.setTaskState(task, state);
    final String realId = getRealId(task.getId());
    if (realId == null) return;
    String url = API_URL + "/projects/" + myProjectId + "/stories/" + realId;
    url +="?" + encodeUrl("story[current_state]") + "=" + encodeUrl("started");
    LOG.info("Updating issue state by id: " + url);
    final HttpMethod method = doREST(url, HTTPMethod.PUT);
    final InputStream stream = method.getResponseBodyAsStream();
    final Element element = new SAXBuilder(false).build(stream).getRootElement();
    final Task story = element.getName().equals("story") ? createIssue(element) : null;
    if (story == null) {
      throw new Exception("Error setting state for: " + url + ", HTTP status code: " + method.getStatusCode() +
                                "\n" + element.getText());
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (!super.equals(o)) return false;
    if (!(o instanceof PivotalTrackerRepository)) return false;

    final PivotalTrackerRepository that = (PivotalTrackerRepository)o;
    if (getAPIKey() != null ? !getAPIKey().equals(that.getAPIKey()) : that.getAPIKey() != null) return false;
    if (getProjectId() != null ? !getProjectId().equals(that.getProjectId()) : that.getProjectId() != null) return false;
    if (getCommitMessageFormat() != null ? !getCommitMessageFormat().equals(that.getCommitMessageFormat()) : that.getCommitMessageFormat() != null) return false;
    return isShouldFormatCommitMessage() == that.isShouldFormatCommitMessage();
  }
}

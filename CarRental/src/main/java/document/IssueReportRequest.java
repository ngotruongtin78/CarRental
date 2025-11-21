package CarRental.example.document;

public class IssueReportRequest {
    private String issue;
    private String severity;

    public IssueReportRequest() {
    }

    public IssueReportRequest(String issue, String severity) {
        this.issue = issue;
        this.severity = severity;
    }

    public String getIssue() {
        return issue;
    }

    public void setIssue(String issue) {
        this.issue = issue;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}


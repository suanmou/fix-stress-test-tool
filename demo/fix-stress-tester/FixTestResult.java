import java.time.Duration;
import java.time.Instant;

public class FixTestResult {
    private final long responseTimeMs;
    private final boolean success;
    private final String errorMessage;
    private final Instant timestamp;

    public FixTestResult(long responseTimeMs, boolean success, String errorMessage) {
        this.responseTimeMs = responseTimeMs;
        this.success = success;
        this.errorMessage = errorMessage;
        this.timestamp = Instant.now();
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
    
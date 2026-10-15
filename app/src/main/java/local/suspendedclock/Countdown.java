package local.suspendedclock;

/** Duration deadlines use elapsed realtime, including sleep; target deadlines align to wall time. */
public final class Countdown {
    public long duration;
    public long remaining;
    public long deadline;
    public boolean running;
    public boolean finished;
    public int boot;
    public boolean target;
    public long targetWall;

    public Countdown(long duration) {
        this.duration = duration;
        this.remaining = duration;
    }

    public long remainingAt(long elapsed) { return running ? Math.max(0, deadline - elapsed) : remaining; }
    public void start(long elapsed) {
        if (running) return;
        if (remaining <= 0 || finished) remaining = duration;
        deadline = elapsed + remaining;
        running = true;
        finished = false;
    }
    public void pause(long elapsed) {
        if (!running) return;
        remaining = remainingAt(elapsed);
        running = false;
        finished = remaining == 0;
    }
    public void reset() { running = false; finished = false; remaining = duration; deadline = 0; }
    public boolean finishIfDue(long elapsed) {
        if (!running || elapsed < deadline) return false;
        running = false;
        finished = true;
        remaining = 0;
        return true;
    }
    public void alignTarget(long elapsed, long wall) {
        if (target && running) deadline = elapsed + targetWall - wall;
    }
}

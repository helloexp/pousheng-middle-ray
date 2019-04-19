package com.pousheng.middle.excel;

import com.google.common.collect.ImmutableMap;
import com.pousheng.middle.web.excel.AbstractSimpleTask;
import com.pousheng.middle.web.excel.TaskContainer;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-09 10:53<br/>
 */
@Slf4j
public class TaskContainerTest {
    @Test
    public void test_ShouldRunOneTaskATime() {
        LinkedBlockingQueue<String> q = new LinkedBlockingQueue<>();
        TaskContainer taskContainer = new TaskContainer();
        taskContainer.setWaitWhenOccupied(10);
        taskContainer.start();

        taskContainer.submit(new SimpleNoLoopTask("a", q, 20));
        taskContainer.submit(new SimpleNoLoopTask("b", q, 30));
        taskContainer.join();

        assertThat(q).containsExactly("astart", "astop", "bstart", "bstop");
    }

    @Test
    public void test_ShouldForceKillTask() throws InterruptedException {
        LinkedBlockingQueue<String> q = new LinkedBlockingQueue<>();
        TaskContainer taskContainer = new TaskContainer();
        taskContainer.setWaitWhenOccupied(10);
        taskContainer.start();

        // wait task start
        SimpleNoLoopTask a = new SimpleNoLoopTask("a", q, 10_000);
        a.setId(1);
        taskContainer.submit(a);
        delay(30);

        taskContainer.tryKill(1l, 10l, TimeUnit.MILLISECONDS);

        // wait task start
        taskContainer.submit(new SimpleNoLoopTask("b", q, 30));
        taskContainer.join();

        assertThat(q).containsExactly("astop", "bstart", "bstop").doesNotContain("astart");
    }

    private static class SimpleNoLoopTask extends AbstractSimpleTask {
        private static final long serialVersionUID = -3831166506710619724L;
        private final String name;
        private final LinkedBlockingQueue<String> q;
        private final long delay;

        SimpleNoLoopTask(String name, LinkedBlockingQueue<String> q, long delay) {
            this.name = name;
            this.q = q;
            this.delay = delay;
        }

        public void setId(long id) {
            this.setMetaInfo(ImmutableMap.of("id", id));
        }

        @Override
        public boolean equalsTo(Object other) {
            return Objects.equals(this.getMetaInfo().get("id"), other);
        }

        @Override
        public Object getTaskKey() {
            return getMetaInfo().get("id");
        }

        @Override
        public Long getTaskId() {
            return (Long) getTaskKey();
        }

        @Override
        public void start() {
            log.info("[TASK {}] starting...", name);
            if (delay(this.delay)) {
                q.offer(name + "start");
            }
        }

        @Override
        public void postStop() {
            log.info("[TASK {}] stopped", name);
            q.offer(name + "stop");
        }
    }

    private static boolean delay(long time) {
        try {
            Thread.sleep(time);
            return true;
        } catch (Exception e) {
            // ignore
            return false;
        }
    }
}

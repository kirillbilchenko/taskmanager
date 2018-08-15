package it.fds.taskmanager;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.fds.taskmanager.dto.TaskDTO;

/**
 * Basic test suite to test the service layer, it uses an in-memory H2 database.
 * <p>
 * TODO Add more and meaningful tests! :)
 *
 * @author <a href="mailto:damiano@searchink.com">Damiano Giampaoli</a>
 * @since 10 Jan. 2018
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class TaskServiceJPATest extends Assert {

	@Autowired
	TaskService taskService;

	@Test
	public void writeAndReadOnDB() {
		TaskDTO t = new TaskDTO();
		t.setTitle("Test task1");
		t.setStatus(TaskState.NEW.toString().toUpperCase());
		TaskDTO t1 = taskService.saveTask(t);
		TaskDTO tOut = taskService.findOne(t1.getUuid());
		assertEquals("Test task1", tOut.getTitle());
		List<TaskDTO> list = taskService.showList();
		assertTrue(list.stream().anyMatch(taskDTO -> taskDTO.getUuid().equals(tOut.getUuid())));
	}

	@Test
	public void postponeTaskTest() {
		List<Integer> integers = Arrays.asList(new Random().nextInt(60), new Random().nextInt(60) * -1,
				10000 + new Random().nextInt(1000));
		integers.stream().forEach(number -> {
			TaskDTO initialTask = new TaskDTO();
			initialTask.setTitle("Postponed task1");
			TaskDTO savedTask = taskService.saveTask(initialTask);
			int postponePeriod = new Random().nextInt(60);
			assertTrue(taskService.postponeTask(savedTask.getUuid(), postponePeriod));
			TaskDTO targetTask = taskService.findOne(savedTask.getUuid());
			int diffMin = (int) Math
					.ceil((targetTask.getPostponedat().getTimeInMillis() - Calendar.getInstance().getTimeInMillis())
							/ (60.0 * 1000));
			assertEquals("Correct difference of time is returned", postponePeriod, diffMin);
			assertEquals(TaskState.POSTPONED.name().toUpperCase(), targetTask.getStatus());
		});
	}

	@Test
	public void resolveTaskTest() {
		TaskDTO initialTask = new TaskDTO();
		initialTask.setTitle("New task");
		TaskDTO savedTask = taskService.saveTask(initialTask);
		assertTrue("Resolve task method successfully launched", taskService.resolveTask(savedTask.getUuid()));
		assertTrue("Resolved task successfully found in task list", taskService.showList().stream()
				.anyMatch(taskDTO -> taskDTO.getStatus().equalsIgnoreCase(TaskState.RESOLVED.name())));
		assertTrue(String.format("Test task %s is successfully resolved", savedTask.getUuid()), StringUtils
				.equalsIgnoreCase(taskService.findOne(savedTask.getUuid()).getStatus(), TaskState.RESOLVED.name()));
	}

	@Test
	public void updateTaskTest() {
		TaskDTO initialTask = new TaskDTO();
		initialTask.setTitle("Update task");
		TaskDTO savedTask = taskService.saveTask(initialTask);
		TaskDTO updatedTask = taskService.updateTask(savedTask);
		assertNotEquals("Updated task is different", savedTask, updatedTask);
		assertNotNull("Updated at date successfully set", updatedTask.getUpdatedat());
	}

	@Test(expected = JpaSystemException.class)
	public void updateTaskNegativeTest() {
		TaskDTO initialTask = new TaskDTO();
		initialTask.setTitle("Update task");
		taskService.updateTask(initialTask);
	}

	@Test
	public void unmarkPostponedTaskTest() {
		TaskDTO initialTask = new TaskDTO();
		initialTask.setTitle("Postponed task");
		TaskDTO savedTask = taskService.saveTask(initialTask);
		int postponePeriod = new Random().nextInt(60);
		assertTrue(taskService.postponeTask(savedTask.getUuid(), postponePeriod));
		assertNotNull(taskService.findOne(savedTask.getUuid()).getPostponedat());
		taskService.unmarkPostoned();
		TaskDTO targetTask = taskService.findOne(savedTask.getUuid());
		assertEquals("Task successfully restored " + targetTask.getUuid(), TaskState.RESTORED.name().toUpperCase(),
				targetTask.getStatus());
		assertNull("Postponed calendar successfully reset", targetTask.getPostponedat());
	}

	@EnableJpaRepositories
	@Configuration
	@SpringBootApplication
	public static class EndpointsMain {
	}
}

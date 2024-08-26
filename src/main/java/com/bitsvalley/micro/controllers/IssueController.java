package com.bitsvalley.micro.controllers;

import com.bitsvalley.micro.domain.*;
import com.bitsvalley.micro.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @author Fru Chifen
 * 11.06.2021
 */
@Controller
public class IssueController extends SuperController {

    @Autowired
    IssueRepository issueRepository;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    TaskCommentRepository taskCommentRepository;

    @Autowired
    UserRepository userRepository;


    @GetMapping(value = "/registerIssue")
    public String registerSaving(ModelMap model, HttpServletRequest request) {
        User user = userRepository.findByUserName(getLoggedInUserName());
        if (user == null) {
            return "findCustomer";
        }
        Issue issue = new Issue();
        model.put("issue", issue);
        List<UserRole> userRoles = new ArrayList<UserRole>();
        getEmployeesInModel(model, user.getOrgId());
        return "issue";
    }

    @PostMapping(value = "/registerIssueForm")
    public String registerIssue(@ModelAttribute("issue") Issue issue, ModelMap model, HttpServletRequest request) {
        User user = userRepository.findByUserName(getLoggedInUserName());
        issue.setBranchCode(user.getBranch().getCode());
        issue.setCreatedBy(user.getUserName());
        issue.setCreatedDate(new Date());
        issue.setOrgId(user.getOrgId());
        issueRepository.save(issue);
        return showIssues(model,issue.getOrgId());
    }

    @GetMapping(value = "/issues")
    public String issues( ModelMap model, HttpServletRequest request) {
        User user = userRepository.findByUserName(getLoggedInUserName());
        showIssues(model, user.getOrgId());
        return "issues";
    }

    @PostMapping(value = "/reloadIssue")
    public String showIssues( ModelMap model, long orgId) {
        List<Issue> issues = issueRepository.findByOrgId(orgId);
        Collections.reverse(issues);
        model.put("issues", issues);
        return "issues";
    }

    @PostMapping(value = "/reloadTask")
    public String showTasks( ModelMap model, long orgId) {
        List<Task> tasks = taskRepository.findByOrgId(orgId);
        Collections.reverse(tasks);
        Issue issue = tasks.iterator().next().getIssue();
        model.put("issue", issue);
        model.put("tasks", tasks);
        return "tasks";
    }

    @GetMapping(value = "/issueDetails/{id}")
    public String issueDetails(@PathVariable("id") long id, ModelMap model) {
        Issue issue = issueRepository.findById(id).get();
        Collections.reverse(issue.getTaskList());
        getEmployeesInModel(model,issue.getOrgId());
        model.put("issue", issue);
        return "issueDetails";
    }

    @GetMapping(value = "/taskDetails/{id}")
    public String taskDetails(@PathVariable("id") long id, ModelMap model) {
        Task task = taskRepository.findById(id).get();
        Collections.reverse(task.getTaskCommentList());
        model.put("task", task);
        return "taskDetails";
    }

    @GetMapping(value = "/registerTask/{id}")
    public String registerTask(@PathVariable("id") long id, ModelMap model) {
        Issue issue = issueRepository.findById(id).get();
        Task task = new Task();
        task.setIssue(issue);
        getEmployeesInModel(model,issue.getOrgId());
        model.put("task", task);
        return "task";
    }

    @PostMapping(value = "/registerTaskForm")
    public String registerTask(@ModelAttribute("task") Task task, ModelMap model, HttpServletRequest request) {
        User user = userRepository.findByUserName(getLoggedInUserName());
        task.setCreatedBy(user.getUserName());
        task.setCreatedDate(new Date());
        String issueId = request.getParameter("issueId");
        Issue issue = issueRepository.findById(new Long(issueId)).get();

        task.setIssue(issue);
        task.setOrgId(user.getOrgId());

        taskRepository.save(task);
        List<Task> taskList = issue.getTaskList();
        if(taskList == null  || taskList.size() == 0) {
            taskList = new ArrayList<Task>();
        }
        taskList.add(task);
        issue.setTaskList(taskList);
        issueRepository.save(issue);
        return issueDetails(issue.getId(), model);
    }

    @PostMapping(value = "/registerCommentForm")
    public String registerComment(@ModelAttribute("task") Task task, ModelMap model, HttpServletRequest request) {
        String issueComment = request.getParameter("issueComment");
        Task aTask = taskRepository.findById(task.getId()).get();

        TaskComment taskComment = new TaskComment();
        taskComment.setNotes(issueComment);
        taskComment.setCreatedBy(getLoggedInUserName());
        taskComment.setCreatedDate(new Date());
        taskComment.setTask(aTask);
//        taskComment.setImageUrl();
        taskCommentRepository.save(taskComment);

        List<TaskComment> taskCommentList = aTask.getTaskCommentList();
        if(taskCommentList == null || taskCommentList.size() == 0 ){
            taskCommentList = new ArrayList<TaskComment>();
        }
        taskCommentList.add(taskComment);
        aTask.setTaskCommentList(taskCommentList);
        taskRepository.save(aTask);
        return taskDetails(aTask.getId(), model);
    }


}
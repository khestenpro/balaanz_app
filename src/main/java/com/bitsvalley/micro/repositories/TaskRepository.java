package com.bitsvalley.micro.repositories;

import com.bitsvalley.micro.domain.Issue;
import com.bitsvalley.micro.domain.Task;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface TaskRepository extends CrudRepository<Task, Long> {

    List<Task> findByOrgId(long orgID);


}

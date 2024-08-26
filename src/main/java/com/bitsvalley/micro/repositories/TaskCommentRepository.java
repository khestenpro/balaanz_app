package com.bitsvalley.micro.repositories;

import com.bitsvalley.micro.domain.Task;
import com.bitsvalley.micro.domain.TaskComment;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface TaskCommentRepository extends CrudRepository<TaskComment, Long> {

}

package com.bitsvalley.micro.repositories;

import com.bitsvalley.micro.domain.Issue;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Objects;

public interface IssueRepository extends CrudRepository<Issue, Long> {

    List<Issue> findByOrgId( long orgID);

    String ISSUE_QUERY = "select priority  , count(id), " +
      "      sum(assigned_to ='') as active_count, " +
      "      sum(assigned_to  != '') as inactive_count " +
      "      from balanz101db.issue i " +
      "      where ticket_status =:ticketStatus " +
      "      AND org_id=:orgId " +
      "      group by priority ;";
    @Query(value = ISSUE_QUERY, nativeQuery = true)
    List<Object[]> findIssuesByFilterting(@Param(value = "ticketStatus") Integer ticketStatus,
                                          @Param(value = "orgId") Integer orgId);

    String ISSUES_BY_DATE = "select\n" +
      "      date_format(created_date, '%Y-%m-%d') as created,\n" +
      "      ticket_status ,\n" +
      "      count(*) as status_count\n" +
      "      from balanz101db.issue\n" +
      "      WHERE org_id=:orgId\n" +
      "      group by created, ticket_status\n" +
      "      order by created, ticket_status;";
    @Query(value = ISSUES_BY_DATE, nativeQuery = true)
    List<Object[]> findIssuesByDateAndStatus(@Param(value = "orgId") Integer orgId);
}

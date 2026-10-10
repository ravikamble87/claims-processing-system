package com.claims.service;

import com.claims.domain.Member;

import java.util.Optional;

/** Port: where members come from (CSV for the demo, the Policy database in production). */
public interface MemberRepository {

    Optional<Member> findMember(long policyHolderId);
}

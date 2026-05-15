package pro.datawiki.auth.base.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.datawiki.auth.base.domain.Role;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
    boolean existsByName(String name);
}

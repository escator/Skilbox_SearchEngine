package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.IndexEntity;

public interface IndexEntityRepository extends JpaRepository<IndexEntity, Integer> {
}

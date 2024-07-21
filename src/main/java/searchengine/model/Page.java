package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Setter @Getter
@Table(name = "page", indexes = @Index(name = "path_index", columnList  =  "path"), uniqueConstraints = {@UniqueConstraint(columnNames = {"path", "site_id"})} )
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ID веб-сайта из таблицы site
    @ManyToOne
    @JoinColumn(name  = "site_id", nullable = false)
    private Site site;

    // адрес страницы от корня сайта (должен начинаться со слэша, например: /news/372189/);
    @Column(columnDefinition = "VARCHAR(255)", name = "path", nullable = false)
    private String path;

    // код HTTP-ответа, полученный при запросе
    //страницы (например, 200, 404, 500 или другие)
    @Column(columnDefinition  =  "INTEGER", name = "title", nullable = false)
    private Integer code;

    // контент страницы (HTML-код).
    @Column(columnDefinition = "MEDIUMTEXT", name = "content", nullable  = false)
    private String content;

}

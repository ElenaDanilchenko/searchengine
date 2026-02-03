package searchengine.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@EqualsAndHashCode(of = {"siteEntity", "path"})
@NoArgsConstructor
@Entity
@Table(name = "page")
public class PageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity siteEntity;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String path;

    @Column(nullable = false)
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    @OneToMany(mappedBy = "pageEntity")
    private Set<IndexEntity> indexEntities;
}

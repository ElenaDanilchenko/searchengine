package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "lemma")
public class LemmaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity siteEntity;

    @Column(nullable = false)
    private String lemma;

    @Column(nullable = false)
    private int frequency;

    @OneToMany(mappedBy = "lemmaEntity")
    private Set<IndexEntity> indexEntities;
}

package ru.daniil.bulletinBoard.entity.base.product;

import jakarta.persistence.*;
import ru.daniil.bulletinBoard.enums.CategoryType;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "tbl_categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private String description;

    @Column(nullable = false)
    private CategoryType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("name ASC")
    private List<Category> children;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Product> products;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Category() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        children = new ArrayList<>();
        products = new ArrayList<>();
    }

    public Category(String name, CategoryType type) {
        this();
        this.name = name;
        this.type = type;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Category(String name, CategoryType type, Category parent) {
        this(name, type);
        this.parent = parent;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CategoryType getType() {
        return type;
    }

    public void setType(CategoryType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Category getParent() {
        return parent;
    }

    public void setParent(Category parent) {
        this.parent = parent;
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isLeaf() {
        return type == CategoryType.LEAF;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public boolean isIntermediate() {
        return type == CategoryType.INTERMEDIATE;
    }

    public void addChild(Category child) {
        child.setParent(this);
        this.children.add(child);
    }

    public void removeChild(Category child) {
        this.children.remove(child);
        child.setParent(null);
    }

    /**
     * Метод для получения полного пути до категории (для проверки корректности связи)
     * @return (string) путь от корневой категории до текущей
     */
    public String getFullPath() {
        if (parent == null) {
            return name;
        }
        return parent.getFullPath() + " > " + name;
    }

    public List<Product> getProducts() { return products; }

    public void setProducts(List<Product> products) {
        if (!isLeaf()) {
            throw new IllegalStateException
                    ("Невозможно указать принадлежность продуктов к не конечной категории");
        }
        this.products = products;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return Objects.equals(id, category.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Category{id=%d, name='%s', type=%s, parent=%s}",
                id, name, type, parent != null ? parent.getName() : "null");
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
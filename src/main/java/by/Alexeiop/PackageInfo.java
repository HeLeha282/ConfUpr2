package by.Alexeiop;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Модель данных для узла в графе зависимостей.
 * Хранит ID, версию, прямые и обратные зависимости.
 */
public class PackageInfo {

    private final String id;
    private final String version;
    private final List<PackageInfo> dependencies; // Прямые зависимости (A -> B) - Этап 3
    private final List<PackageInfo> reverseDependencies; // Обратные зависимости (Пакет, который зависит от текущего) - Этап 4

    // Опциональное поле, если вы используете его для отслеживания разрешения
    private boolean isFullyResolved;

    public PackageInfo(String id, String version) {
        this.id = id;
        this.version = version;
        this.dependencies = new ArrayList<>();
        this.reverseDependencies = new ArrayList<>();
        this.isFullyResolved = false;
    }

    // --- Геттеры ---

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public List<PackageInfo> getDependencies() {
        return dependencies;
    }

    public List<PackageInfo> getReverseDependencies() {
        return reverseDependencies;
    }

    public boolean isFullyResolved() {
        return isFullyResolved;
    }

    // --- Сеттеры и Добавление ---

    public void addDependency(PackageInfo dependency) {
        this.dependencies.add(dependency);
    }

    public void addReverseDependency(PackageInfo dependentPackage) {
        // Добавление обратной зависимости (используется в DependencyGraphBuilder) - Этап 4
        if (!this.reverseDependencies.contains(dependentPackage)) {
            this.reverseDependencies.add(dependentPackage);
        }
    }

    public void setFullyResolved(boolean fullyResolved) {
        isFullyResolved = fullyResolved;
    }

    // --- Переопределение методов (Критично для BFS) ---

    /**
     * Методы equals и hashCode важны для корректной работы Map/Set
     * в алгоритме BFS (resolvedPackages) для обнаружения повторов и циклов.
     * Сравнение пакетов должно производиться по ID и Version.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PackageInfo that = (PackageInfo) o;
        // Два пакета считаются одинаковыми, если их ID и Version совпадают.
        return id.equals(that.id) && version.equals(that.version);
    }

    @Override
    public int hashCode() {
        // Хеширование также должно быть основано на ID и Version.
        return Objects.hash(id, version);
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", id, version);
    }
}
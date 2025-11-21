package by.Alexeiop;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Отвечает за построение графа зависимостей, используя BFS.
 * Обрабатывает транзитивность, глубину, циклы и тестовый режим.
 */
public class DependencyGraphBuilder {

    private final DependencyFetcher fetcher;

    public DependencyGraphBuilder(DependencyFetcher fetcher) {
        this.fetcher = fetcher;
    }

    /**
     * Основной метод для построения графа зависимостей.
     * * @param rootPackageId ID корневого пакета.
     * @param rootPackageVersion Версия корневого пакета.
     * @param repositorySource URL репозитория или путь к тестовому файлу.
     * @param maxDepth Максимальная глубина анализа (null, если не ограничено).
     * @param workingMode Режим работы ("TEST" или "REAL").
     * @return Корневой узел PackageInfo построенного графа.
     */
    public PackageInfo buildDependencyGraph(
            String rootPackageId,
            String rootPackageVersion,
            String repositorySource,
            Integer maxDepth,
            String workingMode) {

        System.out.println("--- Начинаем построение графа зависимостей (Этап 3) ---");

        // 1. Инициализация
        PackageInfo root = new PackageInfo(rootPackageId, rootPackageVersion);

        // Map для отслеживания уже разрешенных пакетов (ID_VERSION -> PackageInfo)
        Map<String, PackageInfo> resolvedPackages = new HashMap<>();
        String rootKey = root.getId() + "_" + root.getVersion();
        resolvedPackages.put(rootKey, root);

        // Очередь для BFS: хранит пару (Пакет, Глубина)
        Queue<AbstractMap.SimpleEntry<PackageInfo, Integer>> queue = new LinkedList<>();
        queue.add(new AbstractMap.SimpleEntry<>(root, 0));

        // 2. Алгоритм BFS
        while (!queue.isEmpty()) {
            AbstractMap.SimpleEntry<PackageInfo, Integer> entry = queue.poll();
            PackageInfo currentPackage = entry.getKey();
            int currentDepth = entry.getValue();

            System.out.printf("Анализ пакета: %s (%s) (Глубина: %d)\n",
                    currentPackage.getId(), currentPackage.getVersion(), currentDepth);

            // Проверка ограничения глубины
            if (maxDepth != null && currentDepth >= maxDepth) {
                System.out.println("   [Глубина] Достигнута максимальная глубина анализа (" + maxDepth + "). Обход прерван.");
                continue;
            }

            List<PackageInfo> directDependencies;

            // 3. Получение прямых зависимостей
            if ("TEST".equalsIgnoreCase(workingMode)) {
                // TEST MODE: Чтение из локального файла
                System.out.println("   [TEST MODE] Поиск зависимостей " + currentPackage.getId() + " в файле: " + repositorySource);
                directDependencies = getTestDependencies(currentPackage.getId(), repositorySource);
            } else {
                // REAL MODE: Запрос к NuGet (Этап 2)
                System.out.println("   [REAL MODE] Запрос зависимостей " + currentPackage.getId() + " к репозиторию: " + repositorySource);
                directDependencies = getRealDependencies(currentPackage, repositorySource);
            }

            // Если зависимости не найдены, переходим к следующему элементу
            if (directDependencies.isEmpty() && currentDepth > 0) {
                System.out.println("   [Зависимости] Не найдены или пакет является конечным.");
            }

            // 4. Обработка найденных зависимостей
            for (PackageInfo dep : directDependencies) {
                String depKey = dep.getId() + "_" + dep.getVersion();
                PackageInfo existingDep = resolvedPackages.get(depKey);

                if (existingDep != null) {
                    // Пакет уже существует (повтор или цикл)
                    currentPackage.addDependency(existingDep);

                    // ЭТАП 4: Сбор обратных зависимостей
                    existingDep.addReverseDependency(currentPackage);

                } else {
                    // Новый пакет: добавляем в структуру, Map и очередь BFS
                    resolvedPackages.put(depKey, dep);
                    currentPackage.addDependency(dep);

                    // ЭТАП 4: Сбор обратных зависимостей
                    dep.addReverseDependency(currentPackage);

                    queue.add(new AbstractMap.SimpleEntry<>(dep, currentDepth + 1));
                }
            }
        }

        System.out.println("--- Построение графа завершено. ---");
        return root;
    }

    /**
     * Реализация получения зависимостей для REAL MODE (Этап 2).
     * В этой реализации просто вызывается fetcher.
     */
    private List<PackageInfo> getRealDependencies(PackageInfo pkg, String repositoryUrl) {
        // Здесь предполагается, что fetcher.fetchDirectDependenciesList() возвращает
        // список PackageInfo, полученных из .nuspec по сети.
        try {
            return fetcher.fetchDirectDependenciesList(pkg.getId(), pkg.getVersion(), repositoryUrl);
        } catch (Exception e) {
            System.err.println("Ошибка при получении зависимостей для " + pkg.getId() + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Реализация получения зависимостей для TEST MODE.
     * Читает зависимости из локального файла.
     * Ожидаемый формат: PACKAGE_A -> PACKAGE_B:VERSION_B, PACKAGE_C:VERSION_C
     */
    private List<PackageInfo> getTestDependencies(String packageName, String filePath) {
        List<PackageInfo> dependencies = new ArrayList<>();

        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            String searchName = packageName.toUpperCase();

            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty()) {
                    continue; // Пропускаем комментарии и пустые строки
                }

                // Ищем строку, начинающуюся с "A -> ..."
                if (line.toUpperCase().startsWith(searchName + " -> ")) {

                    // Удаляем возможные комментарии в конце строки
                    if (line.contains("#")) {
                        line = line.substring(0, line.indexOf("#")).trim();
                    }

                    String[] parts = line.split(" -> ", 2);
                    if (parts.length == 2 && !parts[1].trim().isEmpty()) {

                        // Получаем список зависимостей: "B:2.0.0, C:1.5.0"
                        String[] depEntries = parts[1].split(",");

                        for (String depEntry : depEntries) {
                            String trimmedDepEntry = depEntry.trim();

                            if (!trimmedDepEntry.isEmpty()) {
                                // Теперь разделяем имя и версию: "B:2.0.0" -> ["B", "2.0.0"]
                                String[] depParts = trimmedDepEntry.split(":", 2);

                                String depId = depParts[0].trim();
                                String depVersion = depParts.length > 1 ? depParts[1].trim() : "1.0.0"; // Версия по умолчанию, если не указана

                                dependencies.add(new PackageInfo(depId, depVersion));
                            }
                        }
                    }
                    break; // Нашли зависимости для нужного пакета, выходим
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка при чтении файла тестового репозитория: " + e.getMessage());
        }
        return dependencies;
    }
}
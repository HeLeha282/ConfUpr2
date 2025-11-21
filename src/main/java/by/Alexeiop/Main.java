package by.Alexeiop;

import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Set;

public class Main {

    /**
     * Вывод ASCII-дерева.
     */
    private static void printGraph(PackageInfo root, String prefix, boolean isTail, Set<PackageInfo> visitedForPrint) {
        boolean isRepetition = visitedForPrint.contains(root);
        System.out.println(prefix + (isTail ? "└── " : "├── ") + root.getId() + " (" + root.getVersion() + ")" + (isRepetition ? " [ПОВТОР/ЦИКЛ]" : ""));
        if (isRepetition) return;
        visitedForPrint.add(root);
        List<PackageInfo> dependencies = root.getDependencies();
        for (int i = 0; i < dependencies.size(); i++) {
            boolean isLast = (i == dependencies.size() - 1);
            printGraph(dependencies.get(i), prefix + (isTail ? "    " : "│   "), isLast, visitedForPrint);
        }
    }

    /**
     * Поиск обратных зависимостей (Этап 4).
     */
    private static void printReverseDependencies(PackageInfo root, String targetPackageId) {
        System.out.println("\n*** Обратные зависимости для пакета: " + targetPackageId + " ***");
        Queue<PackageInfo> queue = new LinkedList<>();
        Set<PackageInfo> visited = new HashSet<>();
        queue.add(root);
        visited.add(root);
        PackageInfo targetPackage = null;
        while (!queue.isEmpty()) {
            PackageInfo current = queue.poll();
            // Ищем по частичному совпадению или точному имени
            if (current.getId().toLowerCase().contains(targetPackageId.toLowerCase())) {
                targetPackage = current;
                break;
            }
            for (PackageInfo dep : current.getDependencies()) {
                if (visited.add(dep)) queue.add(dep);
            }
        }
        if (targetPackage == null) {
            System.out.println("Пакет '" + targetPackageId + "' не найден в графе.");
            return;
        }
        List<PackageInfo> reverseDeps = targetPackage.getReverseDependencies();
        if (reverseDeps.isEmpty()) {
            System.out.println("Обратные зависимости не найдены.");
        } else {
            System.out.println("Пакеты, зависящие от " + targetPackage.getId() + ":");
            reverseDeps.forEach(dep -> System.out.printf("- %s\n", dep.getId()));
        }
    }

    public static void main(String[] args) {
        String nameAnalysisPackage = null;
        String urlAddressRepository = null;
        String workingModeWithTestRepository = null;
        String versionPackage = null;
        String nameFileWithImageGraph = null;
        String ModePrintDependency = null;
        Integer maxDepthAnalysisDependencies = null;

        Set<String> paramSet = Set.of("-nameAnalysisPackage", "-urlAddressRepository", "-workingModeWithTestRepository",
                "-versionPackage", "-nameFileWithImageGraph", "-ModePrintDependency", "-maxDepthAnalysisDependencies");

        // Парсинг аргументов
        for (int i = 0; i < args.length; i++) {
            if (paramSet.contains(args[i]) && i + 1 < args.length) {
                String val = args[i + 1];
                switch (args[i]) {
                    case "-nameAnalysisPackage": nameAnalysisPackage = val; break;
                    case "-urlAddressRepository": urlAddressRepository = val; break;
                    case "-workingModeWithTestRepository": workingModeWithTestRepository = val; break;
                    case "-versionPackage": versionPackage = val; break;
                    case "-nameFileWithImageGraph": nameFileWithImageGraph = val; break;
                    case "-ModePrintDependency": ModePrintDependency = val; break;
                    case "-maxDepthAnalysisDependencies": maxDepthAnalysisDependencies = Integer.valueOf(val); break;
                }
                i++;
            }
        }

        System.out.println("Параметры считаны. Начинаем работу...");

        if (nameAnalysisPackage == null || versionPackage == null || urlAddressRepository == null) {
            System.err.println("Ошибка: Не заданы обязательные параметры.");
            return;
        }

        // --- ЭТАП 3: Построение ---
        DependencyFetcher fetcher = new DependencyFetcher();
        DependencyGraphBuilder graphBuilder = new DependencyGraphBuilder(fetcher);

        PackageInfo dependencyGraph = graphBuilder.buildDependencyGraph(
                nameAnalysisPackage, versionPackage, urlAddressRepository,
                maxDepthAnalysisDependencies, workingModeWithTestRepository);

        if (dependencyGraph != null) {
            // --- ЭТАП 3: Вывод дерева ---
            if ("tree".equalsIgnoreCase(ModePrintDependency)) {
                System.out.println("\n*** Граф зависимостей (ASCII-дерево) ***");
                printGraph(dependencyGraph, "", true, new HashSet<>());
            }

            // --- ЭТАП 4: Обратные зависимости (Демонстрация) ---
            // Пытаемся найти обратные зависимости для пакета "B" (если он есть в графе)
            printReverseDependencies(dependencyGraph, "B");

            // --- ЭТАП 5: Визуализация (Graphviz + Mermaid) ---
            if (nameFileWithImageGraph != null) {
                System.out.println("\n--- Генерация визуализации (Этап 5) ---");

                // 1. Graphviz (PNG)
                GraphvizGenerator gGenerator = new GraphvizGenerator();
                gGenerator.generateAndSaveDotFile(dependencyGraph, nameFileWithImageGraph);

                // 2. Mermaid (Текст)
                MermaidGenerator mGenerator = new MermaidGenerator();
                mGenerator.generateAndSaveMermaidFile(dependencyGraph, nameFileWithImageGraph);
            }
        }
        System.out.println("\nРабота завершена.");
    }
}
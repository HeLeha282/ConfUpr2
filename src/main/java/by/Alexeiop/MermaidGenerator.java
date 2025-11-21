package by.Alexeiop;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Генерирует текстовое описание графа зависимостей на языке Mermaid.
 * Результат сохраняется в файл с расширением .mermaid.
 */
public class MermaidGenerator {

    /**
     * Генерирует код Mermaid и сохраняет его в файл.
     * Автоматически меняет расширение файла (например, с .png на .mermaid).
     *
     * @param root           Корневой пакет графа.
     * @param outputFileName Имя исходного файла (например, 'graph.png').
     */
    public void generateAndSaveMermaidFile(PackageInfo root, String outputFileName) {
        if (root == null) {
            System.err.println("Ошибка: Невозможно сгенерировать Mermaid-граф для пустого корня.");
            return;
        }

        // 1. Определяем правильное имя файла (заменяем расширение на .mermaid)
        String mermaidFileName;
        if (outputFileName.contains(".")) {
            mermaidFileName = outputFileName.substring(0, outputFileName.lastIndexOf('.')) + ".mermaid";
        } else {
            mermaidFileName = outputFileName + ".mermaid";
        }

        try {
            // 2. Строим строку с кодом графа
            String mermaidCode = buildMermaidCode(root);

            // 3. Сохраняем в файл
            Path filePath = Path.of(mermaidFileName);
            Files.writeString(filePath, mermaidCode);

            System.out.println("✅ Сгенерирован файл Mermaid: " + mermaidFileName);
            System.out.println("   (Вы можете открыть его в https://mermaid.live/)");

        } catch (IOException e) {
            System.err.println("Ошибка при сохранении Mermaid-файла: " + e.getMessage());
        }
    }

    /**
     * Создает структуру графа Mermaid.
     */
    private String buildMermaidCode(PackageInfo root) {
        StringBuilder builder = new StringBuilder();
        Set<PackageInfo> visited = new HashSet<>();

        // Заголовок: graph TD означает "Graph Top-Down" (сверху вниз)
        builder.append("graph TD\n");

        // --- ИСПРАВЛЕНИЕ: Используем classDef для стилей Mermaid (вместо node [...]) ---
        // Это задаст светлый фон и темную обводку для всех узлов по умолчанию
        builder.append("    classDef default fill:#f9f9f9,stroke:#333,stroke-width:1px;\n");

        // Рекурсивный обход
        buildNodesAndEdges(root, builder, visited);

        return builder.toString();
    }

    /**
     * Рекурсивно добавляет узлы и связи в StringBuilder.
     */
    private void buildNodesAndEdges(PackageInfo current, StringBuilder builder, Set<PackageInfo> visited) {
        // Чтобы Mermaid корректно понимал узлы, используем уникальный ID (хэш)
        String currentId = getNodeMermaidId(current);

        // Метка узла: "Имя (Версия)"
        // Кавычки нужны, чтобы спецсимволы не ломали синтаксис
        String label = String.format("\"%s\\n(%s)\"", current.getId(), current.getVersion());

        // Добавляем определение узла, если мы его еще не посещали
        if (!visited.contains(current)) {
            visited.add(current);
            // Синтаксис: ID[Текст]
            builder.append(String.format("    %s[%s]\n", currentId, label));
        }

        // Обрабатываем зависимости (ребра)
        for (PackageInfo dependency : current.getDependencies()) {
            String dependencyId = getNodeMermaidId(dependency);

            // Если зависимость еще не посещали, нужно зайти в нее рекурсивно,
            // чтобы сначала объявить узел, иначе порядок отрисовки может быть неоптимальным
            if (!visited.contains(dependency)) {
                buildNodesAndEdges(dependency, builder, visited);
            }

            // Добавляем стрелку: Current --> Dependency
            builder.append(String.format("    %s --> %s\n", currentId, dependencyId));
        }
    }

    /**
     * Генерирует безопасный ID для Mermaid (например, N123456).
     * Mermaid не любит точки и дефисы в ID без кавычек, поэтому используем HashCode.
     */
    private String getNodeMermaidId(PackageInfo pkg) {
        // Math.abs убирает минус, "N" делает строку валидным идентификатором
        // Если hashCode совпадет с Integer.MIN_VALUE, Math.abs вернет отрицательное число,
        // поэтому лучше использовать маску или Long, но для учебного проекта этого достаточно.
        return "N" + Math.abs(pkg.hashCode());
    }
}
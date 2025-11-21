package by.Alexeiop;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Генерирует DOT-файл для визуализации графа зависимостей с помощью Graphviz
 * и автоматически компилирует его в изображение PNG/SVG.
 */
public class GraphvizGenerator {

    // Предполагаем, что Graphviz установлен и команда 'dot' находится в переменной PATH.
    private static final String GRAPHVIZ_COMMAND = "dot";

    /**
     * Генерирует DOT-код, сохраняет его, а затем вызывает Graphviz для создания изображения.
     * @param root Корневой пакет графа.
     * @param outputFileName Имя выходного файла (например, 'graph.png').
     */
    public void generateAndSaveDotFile(PackageInfo root, String outputFileName) {
        if (root == null) {
            System.err.println("Ошибка: Невозможно сгенерировать граф для пустого корневого пакета.");
            return;
        }

        // 1. Определяем имена файлов
        // Убеждаемся, что выходной файл имеет расширение для изображения, иначе по умолчанию используем .png
        String imageFileName = outputFileName.toLowerCase().endsWith(".png") || outputFileName.toLowerCase().endsWith(".svg") ?
                outputFileName : outputFileName + ".png";

        // Временный DOT-файл будет иметь имя выходного файла + .dot
        String dotFileName = imageFileName + ".dot";

        try {
            // 2. Генерируем и сохраняем DOT-файл
            String dotCode = buildDotCode(root);
            Path dotFilePath = Path.of(dotFileName);
            Files.writeString(dotFilePath, dotCode);

            System.out.println("✅ Создан временный DOT-файл: " + dotFileName);

            // 3. Компилируем DOT в изображение с помощью внешней команды
            compileDotToImage(dotFileName, imageFileName);

            // 4. Опционально: Удаление временного DOT-файла
            // Files.delete(dotFilePath);

        } catch (IOException e) {
            System.err.println("Критическая ошибка при сохранении/компиляции графа: " + e.getMessage());
        }
    }

    /**
     * Рекурсивно строит DOT-код.
     */
    private String buildDotCode(PackageInfo root) {
        StringBuilder builder = new StringBuilder();
        // Set для отслеживания уже обработанных узлов (чтобы избежать дублирования определения узла и бесконечных циклов)
        Set<PackageInfo> visitedNodes = new HashSet<>();

        builder.append("digraph DependencyGraph {\n");
        builder.append("    rankdir=TB; // Граф сверху вниз\n");
        builder.append("    node [shape=box, style=\"filled,rounded\", color=\"#333333\", fillcolor=\"#EBEBEB\", fontname=\"Helvetica\"];\n");
        builder.append("    edge [color=\"#888888\"];\n\n");

        // Рекурсивный обход для построения узлов и ребер
        buildNodesAndEdges(root, builder, visitedNodes);

        builder.append("}\n");
        return builder.toString();
    }

    private void buildNodesAndEdges(PackageInfo current, StringBuilder builder, Set<PackageInfo> visited) {
        // Уникальный ID узла в DOT формате
        String currentDotId = getNodeDotId(current);

        // Проверяем, был ли узел уже обработан в этом обходе
        if (visited.contains(current)) {
            return;
        }
        visited.add(current);

        // 1. Определение узла (Node)
        String label = String.format("%s\\n(%s)", current.getId(), current.getVersion());
        builder.append(String.format("    %s [label=\"%s\"];\n", currentDotId, label));

        // 2. Определение ребер (Edges)
        for (PackageInfo dependency : current.getDependencies()) {
            String dependencyDotId = getNodeDotId(dependency);

            // Ребро (зависимость): current -> dependency
            builder.append(String.format("    %s -> %s;\n", currentDotId, dependencyDotId));

            // Рекурсивный вызов для транзитивной зависимости
            buildNodesAndEdges(dependency, builder, visited);
        }
    }

    /**
     * Создает уникальный и безопасный для DOT идентификатор узла.
     */
    private String getNodeDotId(PackageInfo pkg) {
        // Заменяем точки и дефисы на подчеркивания для создания валидного идентификатора DOT
        return pkg.getId().replace('.', '_').replace('-', '_') + "_" + pkg.getVersion().replace('.', '_');
    }

    /**
     * Вызывает внешнюю команду Graphviz для преобразования DOT в изображение.
     */
    private void compileDotToImage(String dotFile, String imageFile) {
        // Определяем формат изображения из имени файла
        String format = "png";
        if (imageFile.toLowerCase().endsWith(".svg")) {
            format = "svg";
        }

        try {
            // Сборка команды: dot -Tpng mygraph.dot -o mygraph.png
            ProcessBuilder pb = new ProcessBuilder(
                    GRAPHVIZ_COMMAND,
                    "-T" + format,
                    dotFile,
                    "-o",
                    imageFile
            );

            // Запуск процесса
            Process process = pb.start();

            // Ожидание завершения процесса
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("🎉 Успех: Изображение графа сохранено в: " + imageFile);
            } else {
                // Если Graphviz не найден или произошла ошибка компиляции
                String error = new String(process.getErrorStream().readAllBytes());
                System.err.println("❌ Ошибка при выполнении Graphviz (код " + exitCode + "): " + error);
                System.err.println("   Возможно, Graphviz (команда '" + GRAPHVIZ_COMMAND + "') не установлен или не находится в переменной PATH.");
            }
        } catch (IOException e) {
            System.err.println("❌ Ошибка выполнения команды Graphviz. Убедитесь, что 'dot' установлен и доступен в PATH.");
            System.err.println("Детали ошибки: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Процесс Graphviz был прерван.");
        }
    }
}
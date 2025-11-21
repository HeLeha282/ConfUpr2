package by.Alexeiop;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Отвечает за получение метаданных пакета из реального репозитория NuGet.
 * Выполняет сетевые запросы, скачивает и парсит .nuspec.
 */
public class DependencyFetcher {

    private final HttpClient httpClient;
    private static final String PACKAGE_BASE_ADDRESS_TYPE = "PackageBaseAddress/3.0.0";
    private String packageBaseUrl = null;

    public DependencyFetcher() {
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Ищет URL PackageBaseAddress в serviceIndex.json.
     * @param serviceIndexUrl URL корневого индекса репозитория (например, https://api.nuget.org/v3/index.json).
     */
    private void discoverPackageBaseUrl(String serviceIndexUrl) throws IOException, InterruptedException {
        if (packageBaseUrl != null) {
            return;
        }

        System.out.println("   [NuGet] Обнаружение PackageBaseAddress...");
        HttpRequest request = HttpRequest.newBuilder(URI.create(serviceIndexUrl))
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Не удалось загрузить индекс сервиса. Код: " + response.statusCode());
        }

        // В реальном проекте здесь должен быть полноценный JSON-парсер (Jackson/Gson).
        // Здесь используется очень простой, ненадёжный парсинг строк для демонстрации.
        String json = response.body();
        String searchString = "\"@type\": \"" + PACKAGE_BASE_ADDRESS_TYPE + "\"";
        int typeIndex = json.indexOf(searchString);

        if (typeIndex == -1) {
            throw new IOException("PackageBaseAddress/3.0.0 не найден в индексе сервиса.");
        }

        int idIndex = json.indexOf("\"@id\":", typeIndex);
        int startIndex = json.indexOf("\"", idIndex + 6) + 1;
        int endIndex = json.indexOf("\"", startIndex);

        if (startIndex != -1 && endIndex != -1) {
            packageBaseUrl = json.substring(startIndex, endIndex);
            System.out.println("   [NuGet] Base URL найден: " + packageBaseUrl);
        } else {
            throw new IOException("Не удалось извлечь URL PackageBaseAddress.");
        }
    }

    /**
     * Основной метод: получает список прямых зависимостей для заданного пакета.
     */
    public List<PackageInfo> fetchDirectDependenciesList(String packageId, String version, String serviceIndexUrl) throws Exception {
        discoverPackageBaseUrl(serviceIndexUrl);

        // Формируем URL для скачивания .nupkg
        // Пример: https://api.nuget.org/v3-flatcontainer/newtonsoft.json/13.0.1/newtonsoft.json.13.0.1.nupkg
        String url = String.format("%s%s/%s/%s.%s.nupkg",
                packageBaseUrl,
                packageId.toLowerCase(),
                version.toLowerCase(),
                packageId.toLowerCase(),
                version.toLowerCase());

        System.out.println("   [NuGet] Скачивание пакета: " + url);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            // Если пакет не найден (404), он, вероятно, является мета-пакетом без nuspec.
            System.out.println("   [NuGet] Ошибка скачивания или пакет не найден. Код: " + response.statusCode());
            return Collections.emptyList();
        }

        return parseNuspecFromNupkg(response.body(), packageId);
    }

    /**
     * Открывает .nupkg (ZIP-файл) и извлекает зависимости из .nuspec.
     */
    private List<PackageInfo> parseNuspecFromNupkg(InputStream nupkgStream, String packageId) throws Exception {
        List<PackageInfo> dependencies = new ArrayList<>();

        try (ZipInputStream zipIs = new ZipInputStream(nupkgStream)) {
            ZipEntry entry;
            // Имя nuspec: "Newtonsoft.Json.nuspec" или "package.nuspec"
            String nuspecName = packageId.toLowerCase() + ".nuspec";

            while ((entry = zipIs.getNextEntry()) != null) {
                // Ищем файл метаданных (он может быть в корне или в подпапке)
                if (entry.getName().toLowerCase().endsWith(".nuspec") && entry.getName().toLowerCase().contains(nuspecName)) {

                    dependencies.addAll(parseNuspecXml(zipIs));
                    return dependencies;
                }
            }
        }

        System.out.println("   [NuGet] Файл .nuspec не найден в пакете.");
        return dependencies;
    }

    /**
     * Парсит XML-файл .nuspec для извлечения зависимостей.
     */
    private List<PackageInfo> parseNuspecXml(InputStream xmlStream) throws Exception {
        List<PackageInfo> dependencies = new ArrayList<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlStream);
        doc.getDocumentElement().normalize();

        // Ищем узел <dependencies>
        NodeList depGroups = doc.getElementsByTagName("group");

        // NuGet разделяет зависимости по целевым фреймворкам (group targetFramework="...")
        for (int i = 0; i < depGroups.getLength(); i++) {
            Element group = (Element) depGroups.item(i);

            // Внимание: Здесь происходит упрощение. В реальной жизни нужно выбрать
            // подходящую группу (например, .NET Standard 2.0 или .NET Core 3.1).
            // Для целей демонстрации мы берем зависимости из всех групп.

            NodeList dependencyNodes = group.getElementsByTagName("dependency");
            for (int j = 0; j < dependencyNodes.getLength(); j++) {
                Element dep = (Element) dependencyNodes.item(j);
                String id = dep.getAttribute("id");
                String versionRange = dep.getAttribute("version");

                if (id != null && !id.isEmpty() && versionRange != null && !versionRange.isEmpty()) {
                    // Используем упрощенный подход: берем минимальную версию из диапазона.
                    // Например, для "[4.3.0, )" берем "4.3.0".
                    String version = extractMinVersion(versionRange);
                    dependencies.add(new PackageInfo(id, version));
                }
            }
        }
        return dependencies;
    }

    /**
     * Извлекает минимально необходимую версию из строки диапазона NuGet.
     * (Очень упрощенная реализация для целей проекта)
     * e.g., "[4.3.0, )" -> "4.3.0", "4.3.0" -> "4.3.0"
     */
    private String extractMinVersion(String versionRange) {
        if (versionRange.startsWith("[") || versionRange.startsWith("(")) {
            int commaIndex = versionRange.indexOf(',');
            String minVersion = (commaIndex != -1) ? versionRange.substring(1, commaIndex).trim() : versionRange.substring(1).trim();
            // Удаляем возможную закрывающую скобку, если нет запятой
            if (minVersion.endsWith("]") || minVersion.endsWith(")")) {
                minVersion = minVersion.substring(0, minVersion.length() - 1);
            }
            return minVersion.isEmpty() ? "0.0.0" : minVersion;
        }
        return versionRange; // Если это просто точная версия
    }
}
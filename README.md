# Folder Monitor — User Guide

## WHAT THE APPLICATION DOES

Folder Monitor is a desktop application designed to monitor the size and contents of one or more folders over time. It periodically collects statistics, stores historical data, and presents the results through interactive charts and visual summaries.

The application runs in the background and is optimized to minimize CPU, memory, and disk usage when its main window is closed.

### Key Features

- **Folder Monitoring:** Monitor multiple folders simultaneously, with independent statistics and historical records for each folder.
- **Automatic Data Collection:** Collect folder statistics at configurable intervals, once per day by default.
- **Detailed Statistics:** Track total folder size, number of files and subfolders, images, videos, and other files.
- **Subfolder Analysis:** Monitor first-level subfolders that exceed 500 MB or contain more than 500 files.
- **Historical Data:** Store measurements in CSV files so that statistics remain available between application sessions.
- **Interactive Charts:** Visualize changes in folder size, file counts, folder counts, growth rates, and folder composition over time.
- **Folder Comparison:** Compare the size and growth of multiple monitored folders.
- **Background Operation:** Minimize the application to the system tray without stopping monitoring.
- **Automatic Startup:** Optionally launch the application automatically when Windows starts.
- **Resource Efficiency:** Perform statistical collection in the background and load historical data for visualization only when needed.
- **Persistent Settings:** Remember monitored folders, collection intervals, and application preferences between sessions.
- **Error Handling:** Detect inaccessible folders and handle file access errors without interrupting the monitoring of other folders.

### Statistics Collected

For each monitored folder, the application records:

- Total size in gigabytes.
- Total number of files and subfolders.
- Number of image files.
- Number of video files.
- Number of other files.

Hidden and system files are included whenever they are accessible.

For qualifying first-level subfolders, the application additionally records their size, file count, and subfolder count. Deeper nested folders are included in their parent folder's statistics but are not tracked separately.

### Available Visualizations

- Folder size over time.
- File and subfolder counts over time.
- Growth index comparing changes in folder size and file count.
- Average growth rate over time.
- File-type distribution.
- First-level subfolder distribution by size and file count.
- Ranking of subfolders by growth.
- Comparison of current sizes and historical growth across multiple folders.

Charts can be displayed for the last 7, 30, or 90 days, the entire available history, or a custom period. Summary cards provide current size, changes over the last week and month, and the number of recorded measurements.

**Folder Monitor is read-only:** it analyzes the contents of monitored folders without modifying or deleting their files.

---

## HOW TO USE THE APPLICATION

### 1. Launch the Application

Start Folder Monitor on your Windows computer.

The application opens its main window, where you can configure monitored folders, control data collection, and access historical statistics.

### 2. Add Folders to Monitor

1. Click **Add** to open the folder selection dialog.
2. Select the folder you want to monitor.
3. Repeat the process to add more folders if needed.

Each folder has its own statistics and measurement history.

### 3. Control Monitoring

Use the **Start** and **Stop** buttons next to each folder to enable or pause its data collection independently.

The application periodically scans monitored folders and records the collected statistics. The default collection interval is once every 24 hours, and the interval can be changed in the application settings.

Folder availability and collection errors are reflected in the folder's status and application logs.

### 4. Configure Background Operation

Folder Monitor can continue collecting statistics while its main window is closed.

- **Minimize to System Tray:** Enable this option to keep the application running in the background when the window is closed. Use the system tray menu to reopen the window or exit the application.
- **Start with Windows:** Enable this option to launch Folder Monitor automatically when you sign in to Windows.

These settings are saved automatically and remain active between sessions.

### 5. View Charts and Historical Statistics

1. Click **Show Charts** in the main window.
2. Select one or more monitored folders.
3. Choose the chart type and the desired time period.
4. Review the visualization and summary cards.

Use the available navigation controls to return to the previous screen or switch to another chart.

When comparing multiple folders, you can examine their current sizes and historical growth patterns to identify which folders are growing faster.

### 6. Understand the Collected Data

The application stores measurements in CSV files in its data directory. The recorded history is preserved between sessions, allowing you to analyze changes over days, weeks, and months.

Folder sizes are stored at full precision and displayed in gigabytes rounded to one decimal place.

First-level subfolders are tracked separately when they exceed **500 MB** or contain more than **500 files**. Subfolders that do not meet either threshold are not included as separate measurements for that scan, but their contents remain part of the parent folder's statistics.

Files and folders that cannot be accessed are skipped. If a monitored folder becomes unavailable, the application reports the problem rather than recording incorrect zero values.

### 7. Exit the Application

If the application is minimized to the system tray, open its tray menu and select **Exit** to terminate it completely.

Closing the main window while the system tray option is enabled does not stop monitoring.

---

# Folder Monitor — Руководство пользователя

## ЧТО ДЕЛАЕТ ПРИЛОЖЕНИЕ

Folder Monitor — это настольное приложение для наблюдения за размером и содержимым одной или нескольких папок в течение длительного времени. Оно периодически собирает статистику, сохраняет историю измерений и отображает результаты в виде интерактивных графиков и сводных показателей.

Приложение работает в фоновом режиме и оптимизировано для минимального потребления процессора, оперативной памяти и дисковых ресурсов, когда его главное окно закрыто.

### Основные возможности

- **Мониторинг папок:** одновременное наблюдение за несколькими папками с отдельной статистикой и историей для каждой из них.
- **Автоматический сбор данных:** сбор статистики через заданные интервалы времени; по умолчанию — один раз в сутки.
- **Подробная статистика:** отслеживание общего размера папки, количества файлов и вложенных папок, изображений, видео и остальных файлов.
- **Анализ подпапок:** отдельный учёт подпапок первого уровня, размер которых превышает 500 МБ или в которых содержится более 500 файлов.
- **История измерений:** сохранение данных в CSV-файлах с возможностью просмотра статистики между запусками приложения.
- **Интерактивные графики:** визуализация изменений размера папок, количества файлов и папок, скорости роста и структуры содержимого.
- **Сравнение папок:** сопоставление размеров и динамики роста нескольких наблюдаемых папок.
- **Фоновая работа:** сворачивание приложения в системный трей без остановки мониторинга.
- **Автозапуск:** возможность автоматического запуска приложения при входе в Windows.
- **Экономия ресурсов:** сбор статистики выполняется в фоне, а исторические данные загружаются для отображения графиков только при необходимости.
- **Сохранение настроек:** список наблюдаемых папок, интервалы сбора и параметры приложения сохраняются между запусками.
- **Обработка ошибок:** обнаружение недоступных папок и обработка ошибок доступа к файлам без остановки мониторинга остальных папок.

### Собираемая статистика

Для каждой наблюдаемой папки приложение записывает:

- Общий размер в гигабайтах.
- Общее количество файлов и вложенных папок.
- Количество изображений.
- Количество видео.
- Количество остальных файлов.

Скрытые и системные файлы также учитываются, если к ним есть доступ.

Для подходящих подпапок первого уровня дополнительно записываются размер, количество файлов и количество вложенных папок. Более глубокие уровни входят в статистику родительской папки, но отдельно не отслеживаются.

### Доступные графики

- Изменение размера папки во времени.
- Изменение количества файлов и подпапок.
- Индекс роста, позволяющий сравнивать динамику размера папки и количества файлов.
- Средняя скорость роста со временем.
- Распределение файлов по типам.
- Распределение подпапок первого уровня по размеру и количеству файлов.
- Рейтинг подпапок по приросту.
- Сравнение текущих размеров и исторической динамики нескольких папок.

Для графиков можно выбрать последние 7, 30 или 90 дней, всю доступную историю либо произвольный период. Информационные карточки показывают текущий размер, изменения за последнюю неделю и месяц, а также количество измерений.

**Folder Monitor работает только на чтение:** приложение анализирует содержимое наблюдаемых папок, не изменяя и не удаляя их файлы.

---

## КАК ПОЛЬЗОВАТЬСЯ ПРИЛОЖЕНИЕМ

### 1. Запуск приложения

Запустите Folder Monitor на компьютере с Windows.

Откроется главное окно, в котором можно настроить наблюдаемые папки, управлять сбором статистики и просматривать историю измерений.

### 2. Добавление папок

1. Нажмите **Add (Добавить)**, чтобы открыть диалог выбора папки.
2. Выберите папку, за которой хотите наблюдать.
3. При необходимости добавьте другие папки тем же способом.

Для каждой папки ведётся отдельная статистика и собственная история измерений.

### 3. Управление мониторингом

Используйте кнопки **Start (Старт)** и **Stop (Стоп)** рядом с каждой папкой, чтобы независимо запускать или приостанавливать сбор данных.

Приложение периодически сканирует наблюдаемые папки и записывает полученную статистику. По умолчанию сбор выполняется раз в 24 часа; интервал можно изменить в настройках.

Доступность папок и ошибки сбора отражаются в их статусах и журналах приложения.

### 4. Настройка фоновой работы

Folder Monitor продолжает собирать статистику, даже когда его главное окно закрыто.

- **Minimize to System Tray (Сворачивать в трей):** включите эту опцию, чтобы приложение продолжало работать в фоне после закрытия окна. Через меню системного трея можно снова открыть окно или полностью завершить работу приложения.
- **Start with Windows (Запускать вместе с Windows):** включите эту опцию, чтобы приложение автоматически запускалось при входе в Windows.

Настройки сохраняются автоматически и действуют между запусками.

### 5. Просмотр графиков и истории

1. Нажмите **Show Charts (Показать графики)** в главном окне.
2. Выберите одну или несколько наблюдаемых папок.
3. Выберите тип графика и интересующий период.
4. Изучите график и сводные информационные карточки.

Используйте элементы навигации, чтобы вернуться к предыдущему экрану или переключиться на другой график.

При сравнении нескольких папок можно изучить их текущие размеры и динамику роста, чтобы определить, какие из них увеличиваются быстрее.

### 6. Интерпретация собранных данных

Приложение сохраняет измерения в CSV-файлах в своей папке данных. История сохраняется между запусками, что позволяет анализировать изменения за дни, недели и месяцы.

Размеры папок хранятся с полной точностью и отображаются в гигабайтах с округлением до одного знака после запятой.

Подпапки первого уровня учитываются отдельно, если их размер превышает **500 МБ** или количество файлов превышает **500**. Подпапки, не соответствующие ни одному из условий, не записываются отдельными строками в рамках данного измерения, однако их содержимое продолжает учитываться в статистике родительской папки.

Недоступные файлы и папки пропускаются. Если наблюдаемая папка становится недоступной, приложение сообщает об ошибке вместо записи некорректных нулевых значений.

### 7. Завершение работы

Если приложение свёрнуто в системный трей, откройте его контекстное меню и выберите **Exit (Выход)**, чтобы полностью завершить работу.

Закрытие главного окна не останавливает мониторинг, если включена функция сворачивания в системный трей.

---

# Folder Monitor — Guía del usuario

## QUÉ HACE LA APLICACIÓN

Folder Monitor es una aplicación de escritorio diseñada para supervisar el tamaño y el contenido de una o varias carpetas a lo largo del tiempo. Recopila estadísticas periódicamente, guarda un historial de mediciones y presenta los resultados mediante gráficos interactivos y resúmenes visuales.

La aplicación funciona en segundo plano y está optimizada para minimizar el consumo de CPU, memoria y disco cuando su ventana principal está cerrada.

### Funciones principales

- **Monitorización de carpetas:** supervisión simultánea de varias carpetas, cada una con sus propias estadísticas e historial.
- **Recopilación automática de datos:** registro de estadísticas a intervalos configurables, una vez al día de forma predeterminada.
- **Estadísticas detalladas:** seguimiento del tamaño total, número de archivos y subcarpetas, imágenes, vídeos y otros archivos.
- **Análisis de subcarpetas:** seguimiento independiente de las subcarpetas de primer nivel que superen los 500 MB o contengan más de 500 archivos.
- **Historial de datos:** almacenamiento de las mediciones en archivos CSV para conservar las estadísticas entre sesiones.
- **Gráficos interactivos:** visualización de los cambios en el tamaño de las carpetas, el número de archivos y carpetas, la velocidad de crecimiento y la composición del contenido.
- **Comparación de carpetas:** comparación del tamaño y del crecimiento de varias carpetas supervisadas.
- **Funcionamiento en segundo plano:** minimización de la aplicación en la bandeja del sistema sin detener la monitorización.
- **Inicio automático:** posibilidad de iniciar la aplicación automáticamente al acceder a Windows.
- **Uso eficiente de recursos:** recopilación de estadísticas en segundo plano y carga del historial para los gráficos únicamente cuando es necesario.
- **Configuración persistente:** conservación de las carpetas supervisadas, los intervalos de recopilación y las preferencias entre sesiones.
- **Gestión de errores:** detección de carpetas inaccesibles y tratamiento de errores de acceso sin interrumpir la supervisión de las demás carpetas.

### Estadísticas recopiladas

Para cada carpeta supervisada, la aplicación registra:

- Tamaño total en gigabytes.
- Número total de archivos y subcarpetas.
- Número de imágenes.
- Número de vídeos.
- Número de otros archivos.

Los archivos ocultos y del sistema también se incluyen siempre que sean accesibles.

Para las subcarpetas de primer nivel que cumplen los criterios establecidos, también se registran el tamaño, el número de archivos y el número de subcarpetas. Los niveles más profundos se incluyen en las estadísticas de su carpeta principal, pero no se supervisan por separado.

### Gráficos disponibles

- Evolución del tamaño de una carpeta a lo largo del tiempo.
- Evolución del número de archivos y subcarpetas.
- Índice de crecimiento para comparar los cambios en el tamaño y en el número de archivos.
- Velocidad media de crecimiento a lo largo del tiempo.
- Distribución de los archivos por tipo.
- Distribución de las subcarpetas de primer nivel por tamaño y número de archivos.
- Clasificación de las subcarpetas según su crecimiento.
- Comparación de los tamaños actuales y de la evolución histórica de varias carpetas.

Los gráficos permiten seleccionar los últimos 7, 30 o 90 días, todo el historial disponible o un intervalo personalizado. Las tarjetas de resumen muestran el tamaño actual, los cambios de la última semana y del último mes, y el número de mediciones registradas.

**Folder Monitor funciona en modo de solo lectura:** analiza el contenido de las carpetas supervisadas sin modificar ni eliminar sus archivos.

---

## CÓMO UTILIZAR LA APLICACIÓN

### 1. Iniciar la aplicación

Ejecuta Folder Monitor en tu ordenador con Windows.

Se abrirá la ventana principal, desde la que podrás configurar las carpetas supervisadas, controlar la recopilación de datos y consultar el historial de estadísticas.

### 2. Añadir carpetas para supervisar

1. Haz clic en **Add (Añadir)** para abrir el selector de carpetas.
2. Selecciona la carpeta que deseas supervisar.
3. Repite el proceso para añadir otras carpetas si lo necesitas.

Cada carpeta dispone de sus propias estadísticas y de un historial independiente de mediciones.

### 3. Controlar la monitorización

Utiliza los botones **Start (Iniciar)** y **Stop (Detener)** situados junto a cada carpeta para activar o pausar su recopilación de datos de forma independiente.

La aplicación analiza periódicamente las carpetas supervisadas y registra las estadísticas obtenidas. El intervalo predeterminado es de 24 horas, aunque puedes modificarlo en la configuración.

La disponibilidad de las carpetas y los errores de recopilación se reflejan en sus estados y en los registros de la aplicación.

### 4. Configurar el funcionamiento en segundo plano

Folder Monitor puede seguir recopilando estadísticas aunque su ventana principal esté cerrada.

- **Minimize to System Tray (Minimizar en la bandeja del sistema):** activa esta opción para mantener la aplicación funcionando en segundo plano al cerrar la ventana. Desde el menú de la bandeja podrás volver a abrirla o salir de la aplicación.
- **Start with Windows (Iniciar con Windows):** activa esta opción para iniciar Folder Monitor automáticamente al acceder a Windows.

Estas preferencias se guardan automáticamente y se mantienen entre sesiones.

### 5. Consultar gráficos e historial

1. Haz clic en **Show Charts (Mostrar gráficos)** en la ventana principal.
2. Selecciona una o varias carpetas supervisadas.
3. Elige el tipo de gráfico y el período que deseas analizar.
4. Consulta la visualización y las tarjetas de resumen.

Utiliza los controles de navegación para volver a la pantalla anterior o cambiar de gráfico.

Al comparar varias carpetas, puedes analizar sus tamaños actuales y sus tendencias históricas para identificar cuáles están creciendo más rápidamente.

### 6. Interpretar los datos recopilados

La aplicación guarda las mediciones en archivos CSV dentro de su directorio de datos. El historial se conserva entre sesiones y permite analizar los cambios a lo largo de días, semanas y meses.

Los tamaños se almacenan con precisión completa y se muestran en gigabytes, redondeados a un decimal.

Las subcarpetas de primer nivel se registran por separado cuando superan los **500 MB** o contienen más de **500 archivos**. Las subcarpetas que no cumplen ninguno de estos criterios no generan mediciones independientes en ese análisis, pero su contenido sigue formando parte de las estadísticas de la carpeta principal.

Los archivos y las carpetas inaccesibles se omiten. Si una carpeta supervisada deja de estar disponible, la aplicación informa del problema en lugar de registrar valores cero incorrectos.

### 7. Salir de la aplicación

Si la aplicación está minimizada en la bandeja del sistema, abre su menú contextual y selecciona **Exit (Salir)** para cerrarla completamente.

Cerrar la ventana principal no detiene la monitorización cuando la opción de minimizar en la bandeja del sistema está activada.

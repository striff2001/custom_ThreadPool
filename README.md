# CustomThreadPoolExecutor

---
## Описание проекта

Данный проект реализует кастомную версию пула потоков с настраиваемым управлением очередями, логированием, параметрами и политикой отказа для обеспечения гибкости и эффективности реализации многопоточной среды в высоконагруженных приложениях. <br>
Реализация пула и демонстрационная программа (`Main`) представлены в пакете ``custom_ThreadPool``.

Также в пакете ``performance_research`` реализованы [тесты](#тесты-исследование) для анализа производительности по сравнению со встроенными в Java реализации пулов потоков (`PerformanceTest`) и оптимальных параметров конфигурации кастомного пула потоков (`ParameterTuningTest`).

---
## Алгоритм распределения задач

Распределение задач между потоками реализовано по принципу наименьшей загруженности (Least Loaded).

**Принцип работы**:
1. У каждого потока есть своя очередь задач (BlockingQueue). <br>
2. При добавлении новой задачи метод `getLeastLoadedQueue()` находит очередь с наименьшим количеством задач. <br>
3. Если в найденной очереди есть место, задача добавляется в нее. <br>
4. Если в пуле недостаточно активных потоков, создается новый поток (при условии, что не превышен maxPoolSize). <br>
5. Если все очереди заполнены, срабатывает механизм отказа.

**Преимущества**:
- Равномерное распределение нагрузки между потоками.
- Уменьшение задержек из-за длинных очередей.

**Недостатки**:
- Вариант с `stream().min()` требует перебора всех очередей, что может стать узким местом при очень высоких нагрузках.

---
## Механизм отказа
Если все очереди заполнены и пул не может создать новый поток, применяется следующий механизм отказа:

1. Выводится сообщение:
`[Rejected] Task <...> was rejected due to overload!`

2. Если число активных потоков меньше ``maxPoolSize``, задача выполняется в текущем потоке немедленно.

3. В противном случае:
   - Поток ожидает 8000 мс.
   - Повторно пытается добавить задачу в наименее загруженную очередь.
   - Если повторная попытка не удалась, поток опять ожидает 8000 мс.
   - Затем повторно пытается добавить задачу в наименее загруженную очередь.

   И так пока задача не добавится в очередь

**Преимущества**:
- При перегрузке задачи по возможности обрабатываются немедленно, что предотвращает простаивание вычислительных мощностей.
- Повторная попытка добавления задачи в очередь может помочь, если временная перегрузка была вызвана пиковыми нагрузками.

**Недостатки**:
- Выполнение задачи в текущем потоке может привести к блокировке вызывающего потока, если задача тяжелая.
- Ожидание перед повторной попыткой может немного увеличить задержку в обработке новых задач.


# Тесты (Исследование)

---
## Описание тестов

### 1. **PerformanceTest** (Анализ производительности)
#### Назначение:
Сравнение скорости выполнения задач в **CustomThreadPoolExecutor** и стандартных **ThreadPoolExecutor** из Java.

#### Алгоритм работы теста:
1. Запускает выполнение заданного числа задач в разных реализациях пулов потоков:
   - **CustomThreadPoolExecutor**
   - **FixedThreadPool**
   - **CachedThreadPool**
   - **SingleThreadExecutor**
2. Измеряет общее время выполнения задач для каждого пула.
3. После завершения всех потоков выводит таблицу с результатами.

#### Структура таблицы с результатами:
| ThreadPool Type        | Total Tasks | Execution Time (ms) | Created Threads |
|------------------------|------------|---------------------|-----------------|
| CustomThreadPool      | 1000       | 12500               | 4               |
| FixedThreadPool       | 1000       | 13200               | 4               |
| CachedThreadPool      | 1000       | 11800               | 25              |
| SingleThreadExecutor | 1000       | 50000               | 1               |

- **Total Tasks** – количество выполненных задач.
- **Execution Time (ms)** – общее время выполнения задач в миллисекундах.
- **Created Threads** – количество созданных потоков в пуле.

---

### 2. **ParameterTuningTest** (Оптимизация параметров пула)
#### Назначение:
Определение наиболее эффективных комбинаций параметров для **CustomThreadPoolExecutor**.

#### Алгоритм работы теста:
1. Запускает тесты с разными параметрами пула (**corePoolSize, maxPoolSize, queueSize, keepAliveTime**).
2. Выполняет серию задач и измеряет производительность.
3. После завершения всех потоков выводит таблицу с результатами.

#### Структура таблицы с результатами:
| Core Pool Size | Max Pool Size | Queue Size | Keep Alive (s) | Execution Time (ms) | Created Threads |
|---------------|--------------|------------|----------------|---------------------|-----------------|
| 2             | 4            | 10         | 5              | 12500               | 4               |
| 4             | 8            | 20         | 10             | 9800                | 6               |
| 8             | 16           | 50         | 15             | 9200                | 10              |

- **Core Pool Size** – базовое количество потоков.
- **Max Pool Size** – максимальное количество потоков.
- **Queue Size** – размер очереди задач.
- **Keep Alive (s)** – время ожидания перед завершением потока.
- **Execution Time (ms)** – общее время выполнения задач.
- **Created Threads** – количество созданных потоков в пуле.

---

## Выводы
- **PerformanceTest** показывает, насколько эффективно работает **CustomThreadPoolExecutor** по сравнению со встроенными в Java реализациями.
- **ParameterTuningTest** помогает подобрать лучшие параметры пула.
- Итоговые таблицы позволяют увидеть влияние каждого параметра на производительность.

---

## Возможные улучшения
- Добавить визуализацию данных (графики выполнения).
- Тестировать пул при различных типах нагрузки (IO-задачи, CPU-задачи).
- Реализовать поддержку динамического изменения параметров пула во время работы.


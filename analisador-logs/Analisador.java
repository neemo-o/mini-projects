/*
    Este é o Desafio Master: O Analisador de Logs de Alta Performance.

Ele vai te obrigar a usar Arquivos (IO), Threads (Concorrência), Streams (Funcional) e OOP tudo junto. É um cenário muito comum em empresas que processam grandes volumes de dados.

O Cenário
Você trabalha em uma empresa de servidores. Um sistema caiu e gerou um arquivo de log gigante (servidor.log). Seu chefe precisa de um relatório urgente contendo apenas os erros críticos, mas o arquivo é grande demais para ler manualmente.

Você precisa criar um programa que:

Gere esse arquivo de log (para simular o cenário).

Leia o arquivo do disco.

Filtre e converta as linhas em Objetos Java.

Processe os erros em paralelo (Multithreading) para ser rápido.

Salve um relatório final no disco.

Requisitos Técnicos (O que você vai codar)
1. Modelagem (Enums e POJO)
Crie um Enum NivelLog (INFO, WARNING, ERROR).

Crie uma classe LogEntry com: data (LocalDateTime), nivel (NivelLog) e mensagem (String).

2. (Feito anteriormente);

3. O Leitor (IO + Streams + Parsing)
Leia o arquivo linha por linha.

Use Stream para converter a linha (String) em um objeto LogEntry.

Trate exceções caso uma linha venha corrompida (try-catch dentro do map/loop).

4. O Processador Concorrente (ExecutorService - Nível 5)
Filtre apenas os logs de nível ERROR.

Simule que analisar um erro é pesado: Thread.sleep(100).

Use um ExecutorService com um pool de threads (ex: 5 threads) para processar esses erros simultaneamente.

Conte quantos erros foram processados usando AtomicInteger (para garantir thread-safety).

5. O Relatório (IO Final)
Ao final do processamento das threads, escreva um arquivo relatorio.txt dizendo: "Análise finalizada. Total de Erros Críticos encontrados: X".
*/

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

enum NivelLog {
    INFO,
    WARNING,
    ERROR;
}

class LogEntry {
    private LocalDateTime data;
    private NivelLog nivel;
    private String message;

    public LogEntry(LocalDateTime data, NivelLog nivel, String message) {
        this.data = data;
        this.nivel = nivel;
        this.message = message;
    }

    public NivelLog getNivel() {
        return this.nivel;
    }

    public String toString() {
        return "NIVEL: " + this.nivel + " - DATA: " + this.data + " - MESSAGEM: " + this.message;
    }
}

class AnalisadorService {
    private List<LogEntry> servidor_logs = new ArrayList<>();
    private ExecutorService pool;
    private AtomicInteger cont = new AtomicInteger();
    private File logs;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AnalisadorService(File logs) {
        this.pool = Executors.newFixedThreadPool(5);
        this.logs = logs;
    }

    public void analisar() {
        try {
            servidor_logs = Files.lines(Paths.get(logs.getPath()))
                .map(line -> {
                    try {
                        String[] data = line.split(";");
                        LocalDateTime date = LocalDateTime.parse(data[0], formatter);
                        NivelLog nivel = NivelLog.valueOf(data[1]);
                        return new LogEntry(date, nivel, data[2]);
                    } catch (Exception e) {
                        System.err.println("Linha corrompida ignorada: " + line);
                        return null;
                    }
                })
                .filter(entry -> entry != null)
                .collect(Collectors.toList());

            List<LogEntry> filtered_logs = servidor_logs.stream()
                .filter(e -> e.getNivel() == NivelLog.ERROR)
                .collect(Collectors.toList());

            List<Future<?>> tasks = new ArrayList<>();
            for (LogEntry e : filtered_logs) {
                tasks.add(pool.submit(() -> {
                    try {
                        Thread.sleep(100); // Simula processamento pesado
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    cont.getAndIncrement();
                }));
            }

            for (Future<?> f : tasks) {
                f.get();
            }

            pool.shutdown();
            pool.awaitTermination(10, TimeUnit.MINUTES);
            this.gerarRelatorio();
        } catch (Exception e) {
            System.out.println("Erro ao analisar: " + e.getMessage());
        }
    }



    public void gerarRelatorio() {
        try (FileWriter w = new FileWriter("relatorio.txt")) {
            w.write("Análise finalizada. Total de Erros Críticos encontrados: " + this.cont.get());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

public class Analisador {

    public static void main(String[] args) {
        File servidorLogs = new File("servidor.log");
        AnalisadorService aS = new AnalisadorService(servidorLogs);
        aS.analisar();
    }
}

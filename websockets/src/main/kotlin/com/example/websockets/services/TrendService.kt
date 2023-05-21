package com.example.websockets.services;

import com.example.websockets.dto.ChatMessage
import com.example.websockets.dto.MessageType
import com.example.websockets.dto.NotificationType
import com.example.websockets.dto.TrendsNotification
import com.example.websockets.entities.TrendWord
import com.example.websockets.entities.TrendWordRepository
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
@EnableScheduling
class TrendService (
    val trendRepository: TrendWordRepository,
    val messageSender: SimpMessageSendingOperations
) {
    // https://github.com/Alir3z4/stop-words
    val stopWords = setOf("a", "actualmente", "adelante", "además", "afirmó", "agregó", "ahora", "ahí", "al", "algo",
        "alguna", "algunas", "alguno", "algunos", "algún", "alrededor", "ambos", "ampleamos", "ante", "anterior",
        "antes", "apenas", "aproximadamente", "aquel", "aquellas", "aquellos", "aqui", "aquí", "arriba", "aseguró",
        "así", "atras", "aunque", "ayer", "añadió", "aún", "bajo", "bastante", "bien", "buen", "buena", "buenas",
        "bueno", "buenos", "cada", "casi", "cerca", "cierta", "ciertas", "cierto", "ciertos", "cinco", "comentó",
        "como", "con", "conocer", "conseguimos", "conseguir", "considera", "consideró", "consigo", "consigue",
        "consiguen", "consigues", "contra", "cosas", "creo", "cual", "cuales", "cualquier", "cuando", "cuanto",
        "cuatro", "cuenta", "cómo", "da", "dado", "dan", "dar", "de", "debe", "deben", "debido", "decir", "dejó",
        "del", "demás", "dentro", "desde", "después", "dice", "dicen", "dicho", "dieron", "diferente", "diferentes",
        "dijeron", "dijo", "dio", "donde", "dos", "durante", "e", "ejemplo", "el", "ella", "ellas", "ello", "ellos",
        "embargo", "empleais", "emplean", "emplear", "empleas", "empleo", "en", "encima", "encuentra", "entonces",
        "entre", "era", "erais", "eramos", "eran", "eras", "eres", "es", "esa", "esas", "ese", "eso", "esos", "esta",
        "estaba", "estabais", "estaban", "estabas", "estad", "estada", "estadas", "estado", "estados", "estais",
        "estamos", "estan", "estando", "estar", "estaremos", "estará", "estarán", "estarás", "estaré", "estaréis",
        "estaría", "estaríais", "estaríamos", "estarían", "estarías", "estas", "este", "estemos", "esto", "estos",
        "estoy", "estuve", "estuviera", "estuvierais", "estuvieran", "estuvieras", "estuvieron", "estuviese",
        "estuvieseis", "estuviesen", "estuvieses", "estuvimos", "estuviste", "estuvisteis", "estuviéramos",
        "estuviésemos", "estuvo", "está", "estábamos", "estáis", "están", "estás", "esté", "estéis", "estén", "estés",
        "ex", "existe", "existen", "explicó", "expresó", "fin", "fue", "fuera", "fuerais", "fueran", "fueras", "fueron",
        "fuese", "fueseis", "fuesen", "fueses", "fui", "fuimos", "fuiste", "fuisteis", "fuéramos", "fuésemos", "gran",
        "grandes", "gueno", "ha", "haber", "habida", "habidas", "habido", "habidos", "habiendo", "habremos", "habrá",
        "habrán", "habrás", "habré", "habréis", "habría", "habríais", "habríamos", "habrían", "habrías", "habéis",
        "había", "habíais", "habíamos", "habían", "habías", "hace", "haceis", "hacemos", "hacen", "hacer", "hacerlo",
        "haces", "hacia", "haciendo", "hago", "han", "has", "hasta", "hay", "haya", "hayamos", "hayan", "hayas",
        "hayáis", "he", "hecho", "hemos", "hicieron", "hizo", "hoy", "hube", "hubiera", "hubierais", "hubieran",
        "hubieras", "hubieron", "hubiese", "hubieseis", "hubiesen", "hubieses", "hubimos", "hubiste", "hubisteis",
        "hubiéramos", "hubiésemos", "hubo", "igual", "incluso", "indicó", "informó", "intenta", "intentais",
        "intentamos", "intentan", "intentar", "intentas", "intento", "ir", "junto", "la", "lado", "largo", "las", "le",
        "les", "llegó", "lleva", "llevar", "lo", "los", "luego", "lugar", "manera", "manifestó", "mayor", "me",
        "mediante", "mejor", "mencionó", "menos", "mi", "mientras", "mio", "mis", "misma", "mismas", "mismo", "mismos",
        "modo", "momento", "mucha", "muchas", "mucho", "muchos", "muy", "más", "mí", "mía", "mías", "mío", "míos",
        "nada", "nadie", "ni", "ninguna", "ningunas", "ninguno", "ningunos", "ningún", "no", "nos", "nosotras",
        "nosotros", "nuestra", "nuestras", "nuestro", "nuestros", "nueva", "nuevas", "nuevo", "nuevos", "nunca", "o",
        "ocho", "os", "otra", "otras", "otro", "otros", "para", "parece", "parte", "partir", "pasada", "pasado", "pero",
        "pesar", "poca", "pocas", "poco", "pocos", "podeis", "podemos", "poder", "podria", "podriais", "podriamos",
        "podrian", "podrias", "podrá", "podrán", "podría", "podrían", "poner", "por", "por qué", "porque", "posible",
        "primer", "primera", "primero", "primeros", "principalmente", "propia", "propias", "propio", "propios",
        "próximo", "próximos", "pudo", "pueda", "puede", "pueden", "puedo", "pues", "que", "quedó", "queremos",
        "quien", "quienes", "quiere", "quién", "qué", "realizado", "realizar", "realizó", "respecto", "sabe",
        "sabeis", "sabemos", "saben", "saber", "sabes", "se", "sea", "seamos", "sean", "seas", "segunda", "segundo",
        "según", "seis", "ser", "seremos", "será", "serán", "serás", "seré", "seréis", "sería", "seríais", "seríamos",
        "serían", "serías", "seáis", "señaló", "si", "sido", "siempre", "siendo", "siete", "sigue", "siguiente", "sin",
        "sino", "sobre", "sois", "sola", "solamente", "solas", "solo", "solos", "somos", "son", "soy", "su", "sus",
        "suya", "suyas", "suyo", "suyos", "sí", "sólo", "tal", "también", "tampoco", "tan", "tanto", "te", "tendremos",
        "tendrá", "tendrán", "tendrás", "tendré", "tendréis", "tendría", "tendríais", "tendríamos", "tendrían",
        "tendrías", "tened", "teneis", "tenemos", "tener", "tenga", "tengamos", "tengan", "tengas", "tengo", "tengáis",
        "tenida", "tenidas", "tenido", "tenidos", "teniendo", "tenéis", "tenía", "teníais", "teníamos", "tenían",
        "tenías", "tercera", "ti", "tiempo", "tiene", "tienen", "tienes", "toda", "todas", "todavía", "todo", "todos",
        "total", "trabaja", "trabajais", "trabajamos", "trabajan", "trabajar", "trabajas", "trabajo", "tras", "trata",
        "través", "tres", "tu", "tus", "tuve", "tuviera", "tuvierais", "tuvieran", "tuvieras", "tuvieron", "tuviese",
        "tuvieseis", "tuviesen", "tuvieses", "tuvimos", "tuviste", "tuvisteis", "tuviéramos", "tuviésemos", "tuvo",
        "tuya", "tuyas", "tuyo", "tuyos", "tú", "ultimo", "un", "una", "unas", "uno", "unos", "usa", "usais", "usamos",
        "usan", "usar", "usas", "uso", "usted", "va", "vais", "valor", "vamos", "van", "varias", "varios", "vaya",
        "veces", "ver", "verdad", "verdadera", "verdadero", "vez", "vosotras", "vosotros", "voy", "vuestra", "vuestras",
        "vuestro", "vuestros", "y", "ya", "yo", "él", "éramos", "ésta", "éstas", "éste", "éstos", "última", "últimas",
        "último", "últimos", "ademas", "afirmo", "agrego", "ahi", "algun", "aseguro", "asi", "añadio", "aun", "comento",
        "considero", "dejo", "demas", "despues", "estara", "estaran", "estaras", "estare", "estareis", "estaria",
        "estariais", "estariamos", "estarian", "estarias", "estuvieramos", "estuviesemos", "estabamos", "esteis",
        "esten", "estes", "explico", "expreso", "fueramos", "fuesemos", "habra", "habran", "habras", "habre", "habreis",
        "habria", "habriais", "habriamos", "habrian", "habrias", "habeis", "habia", "habiais", "habiamos", "habian",
        "habias", "hayais", "hubieramos", "hubiesemos", "indico", "informo", "llego", "manifesto", "menciono", "mas",
        "mia", "mias", "mios", "ningun", "podra", "podran", "por que", "proximo", "proximos", "quedo", "realizo",
        "segun", "sera", "seran", "seras", "sere", "sereis", "seria", "seriais", "seriamos", "serian", "serias",
        "seais", "señalo", "tambien", "tendra", "tendran", "tendras", "tendre", "tendreis", "tendria", "tendriais",
        "tendriamos", "tendrian", "tendrias", "tengais", "tenia", "teniais", "teniamos", "tenian", "tenias", "todavia",
        "traves", "tuvieramos", "tuviesemos", "ultima", "ultimas", "ultimos")

    fun addMessage(msg: ChatMessage) {

        // Don't store IDs as words
        if(msg.type == MessageType.ATTACHMENT) return

        val t = LocalDateTime.parse(
            msg.timestamp.substring(0, msg.timestamp.length - 1)
        )
        msg.content.split(" ").forEach {
            if(stopWords.contains(it)) return@forEach
            val w = TrendWord(
                word = it,
                timestamp = Timestamp.valueOf(t),
            )
            trendRepository.save(w)
        }
    }

    @Scheduled(fixedDelay = 20000)
    fun calculateTrends() {
        val ts = Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS))
        val messages = trendRepository.findLastHourWords(ts)
        val wc = messages.groupingBy { it.word }.eachCount()
        messageSender.convertAndSend(
            "/topic/system/trends",
            TrendsNotification(
                NotificationType.TREND_LIST,
                wc.entries.sortedBy { -it.value }.take(10).map { Pair(it.key, it.value) }
            )
        )
    }
}

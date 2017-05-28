package worked_example.`_2_adding_multiple_endpoints`

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.client.OkHttp
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.routing.by
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.junit.After
import org.junit.Before
import org.junit.Test

abstract class EndToEndTest {
    val client = OkHttp()
    val server = MyMathServer(8000)

    @Before
    fun setup(): Unit {
        server.start()
    }

    @After
    fun teardown(): Unit {
        server.stop()
    }
}

class NonFunctionalRequirementsTest : EndToEndTest() {
    @Test
    fun `responds to ping`() {
        client(Request(GET, "http://localhost:8000/ping")).status shouldMatch equalTo(OK)
    }
}

class AddsTest : EndToEndTest() {
    private fun Response.statusShouldBe(expected: Status) = status shouldMatch equalTo(expected)

    private fun Response.answerShouldBe(expected: Int) {
        statusShouldBe(OK)
        bodyString().toInt() shouldMatch equalTo(expected)
    }

    @Test
    fun `adds values together`() {
        client(Request(GET, "http://localhost:8000/add?value=1&value=2")).answerShouldBe(3)
    }

    @Test
    fun `answer is zero when no values`() {
        client(Request(GET, "http://localhost:8000/add")).answerShouldBe(0)
    }

    @Test
    fun `bad request when some values are not numbers`() {
        client(Request(GET, "http://localhost:8000/add?value=1&value=notANumber")).statusShouldBe(BAD_REQUEST)
    }
}

fun MyMathServer(port: Int): Http4kServer {
    val app = routes(
        GET to "/ping" by { _: Request -> Response(OK) },
        GET to "/add" by { request: Request ->
            val valuesToAdd = Query.int().multi.defaulted("value", listOf()).extract(request)
            Response(OK).body(valuesToAdd.sum().toString())
        }
    )
    return CatchLensFailure.then(app).asServer(Jetty(port))
}


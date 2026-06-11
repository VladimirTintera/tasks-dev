package eu.tintera.guard

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class ExhaustibleTokenProducerTest {

    @Test
    fun `emits token initially, exhausts on expire and produces new token when onStarted is called`() = runTest {
        val producer = TestExhaustibleTokenProducer()
        var parentExpireCallCount = 0

        // Spustíme sběr flow přes Turbine
        producer.token().test {
            // --- FÁZE 1: INICIALIZACE ---
            // Okamžitě po napojení bychom měli dostat první token
            val firstToken = awaitItem()

            firstToken.invokeOnPreCancel { parentExpireCallCount++ }

            assertEquals(1, producer.produceCallCount, "Should produce exactly one token initially")

            // --- FÁZE 2: VYČERPÁNÍ (EXHAUSTION) ---
            // Nasimulujeme, že systému došel čas a zavolá interní onExpire()
            producer.capturedExpireCallback.invoke()

            // Ověříme, že se to propsalo i ven (třeba do kompozitu)
            assertEquals(1, parentExpireCallCount, "Parent onExpire should be triggered")

            // Ověříme, že producer je skutečně vyčerpaný a NEPOSÍLÁ žádný další token.
            // Turbine se ujistí, že ve frontě nečeká žádný další emit.
            expectNoEvents()

            // --- FÁZE 3: OŽIVENÍ (RESURRECTION) ---
            // Nasimulujeme, že OS probudil aplikaci (např. přes HealthKit)
            // a lifecycle observer zavolal onStarted()
            producer.onStarted()

            // StateFlow by měl přehodit stav a okamžitě vyžádat a emitnout nový token
            val secondToken = awaitItem()

            secondToken.invokeOnPreCancel { parentExpireCallCount++ }

            assertEquals(2, producer.produceCallCount, "Should produce a second token after resurrection")
            assertEquals(1, parentExpireCallCount, "Parent onExpire count should remain the same for now")
            assertNotSame(firstToken, secondToken, "Should be a completely new token instance")

            // Zrušíme naslouchání (jelikož flow mapované přes StateFlow by jinak běželo donekonečna)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `providedObservers contains self`() {
        val producer = TestExhaustibleTokenProducer()

        // Ověříme, že producer správně nabízí sám sebe jako observera
        assertTrue(
            producer.providedObservers.contains(producer),
            "Producer should expose itself in providedObservers"
        )
    }
}
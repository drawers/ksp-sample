package com.tsongkha.kspexample.processor

import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.intellij.lang.annotations.Language
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntSummableProcessorTest {

    @Rule
    @JvmField
    var temporaryFolder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `target is not a data class`() {
        val kotlinSource = SourceFile.kotlin(
            "file1.kt", """
        package com.tests.summable
        
        import com.tsongkha.kspexample.annotation.IntSummable

          @IntSummable
          class FooSummable(
            val bar: Int = 234,
            val baz: Int = 123
          )
    """
        )

        val compilationResult = compile(kotlinSource)

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, compilationResult.exitCode)
        val expectedMessage = "@IntSummable cannot target non-data class com.tests.summable.FooSummable"
        assertTrue("Expected message containing text $expectedMessage but got: ${compilationResult.messages}") {
            compilationResult.messages.contains(expectedMessage)
        }
    }

    @Test
    fun `target is a data class`() {
        val kotlinSource = SourceFile.kotlin(
            "file1.kt", """
        package com.tests.summable
        
        import com.tsongkha.kspexample.annotation.IntSummable

          @IntSummable
          data class FooSummable(
            val bar: Int = 234,
            val baz: Int = 123
          )
    """
        )

        val compilationResult = compile(kotlinSource)

        assertEquals(KotlinCompilation.ExitCode.OK, compilationResult.exitCode)
        assertSourceEquals(
            """
                package com.tests.summable
                
                import kotlin.Int
                
                public fun FooSummable.sumInts(): Int {
                  val sum = bar + baz
                  return sum
                }""",
            compilationResult.sourceFor("FooSummable.kt")
        )
    }

    private fun compile(vararg source: SourceFile) = KotlinCompilation().apply {
        sources = source.toList()
        symbolProcessorProviders = listOf(IntSummableProcessor.IntSummableProcessorProvider())
        workingDir = temporaryFolder.root
        inheritClassPath = true
        verbose = false
    }.compile()


    private fun assertSourceEquals(@Language("kotlin") expected: String, actual: String) {
        assertEquals(
            expected.trimIndent(),
            // unfortunate hack needed as we cannot enter expected text with tabs rather than spaces
            actual.trimIndent().replace("\t", "    ")
        )
    }

    private fun KotlinCompilation.Result.sourceFor(fileName: String): String {
        return kspGeneratedSources().find { it.name == fileName }
            ?.readText()
            ?: throw IllegalArgumentException("Could not find file $fileName in ${kspGeneratedSources()}")
    }

    private fun KotlinCompilation.Result.kspGeneratedSources(): List<File> {
        val kspWorkingDir = workingDir.resolve("ksp")
        val kspGeneratedDir = kspWorkingDir.resolve("sources")
        val kotlinGeneratedDir = kspGeneratedDir.resolve("kotlin")
        val javaGeneratedDir = kspGeneratedDir.resolve("java")
        return kotlinGeneratedDir.walk().toList() +
                javaGeneratedDir.walk().toList()
    }

    private val KotlinCompilation.Result.workingDir: File
        get() = checkNotNull(outputDirectory.parentFile)
}
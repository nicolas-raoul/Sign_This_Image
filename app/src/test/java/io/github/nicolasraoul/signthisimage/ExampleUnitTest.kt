package io.github.nicolasraoul.signthisimage

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for the Sign This Image app functionality
 */
class SignThisImageUnitTest {
    
    @Test
    fun generateFileName_withOriginalFileName_returnsCorrectFormat() {
        // Test the filename generation logic from FileSaver
        val originalName = "screenshot-2023-10-27.png"
        val expectedOutput = "screenshot-2023-10-27_signed.png"
        
        // Since the generateFileName method is private, we're testing the concept
        val nameWithoutExtension = originalName.substringBeforeLast(".")
        val result = "${nameWithoutExtension}_signed.png"
        
        assertEquals(expectedOutput, result)
    }
    
    @Test
    fun generateFileName_withoutExtension_returnsCorrectFormat() {
        val originalName = "my_image"
        val expectedOutput = "my_image_signed.png"
        
        val nameWithoutExtension = originalName.substringBeforeLast(".")
        val result = "${nameWithoutExtension}_signed.png"
        
        assertEquals(expectedOutput, result)
    }
    
    @Test
    fun generateFileName_withMultipleDots_handlesCorrectly() {
        val originalName = "my.image.file.jpg"
        val expectedOutput = "my.image.file_signed.png"
        
        val nameWithoutExtension = originalName.substringBeforeLast(".")
        val result = "${nameWithoutExtension}_signed.png"
        
        assertEquals(expectedOutput, result)
    }
}
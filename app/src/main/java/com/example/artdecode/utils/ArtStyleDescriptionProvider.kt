package com.example.artdecode.utils

import com.example.artdecode.R // Make sure this import is correct for your project's R file

object ArtStyleDescriptionProvider {

    fun getStyleDescription(artStyle: String?): String {
        return when (artStyle) {
            "Art Nouveau Modern" -> "Flowing, curvilinear lines, organic forms, and decorative motifs inspired by nature, often featuring female figures, flowers, and insects. Popular in architecture, interior design, and graphic arts."
            "Baroque" -> "A highly ornate and extravagant style of architecture, art, and music that flourished in Europe from the early 17th to mid-18th century. It is characterized by grandeur, drama, and emotional intensity, often used to express the power of the Catholic Church."
            "Color Field Painting" -> "A style of Abstract Expressionism characterized by large fields of solid color, stained or painted onto the canvas, to create an intense, unified, and often spiritual experience without discernible imagery."
            "Cubism" -> "An early 20th-century art movement that revolutionized European painting and sculpture. In Cubist artworks, objects are analyzed, broken up and reassembled in an abstracted form—instead of depicting objects from one viewpoint, the artist depicts the subject from a multitude of viewpoints to represent the subject in a greater context."
            "Expressionism" -> "A modernist movement, initially in poetry and painting, originating in Germany at the beginning of the 20th century. Its typical trait is to present the world solely from a subjective perspective, distorting it radically for emotional effect in order to evoke moods or ideas."
            "Fauvism" -> "Known for its daring and intense use of vibrant, often clashing, colors applied directly from the tube, leading to a raw and expressive quality, prioritizing color over realistic representation."
            "Impressionism" -> "A 19th-century art movement characterized by small, thin, yet visible brush strokes, open composition, emphasis on accurate depiction of light in its changing qualities (often accentuating the effects of the passage of time), ordinary subject matter, inclusion of movement as a crucial element of human perception and experience, and unusual visual angles."
            "Minimalism" -> "Reduces art to its most essential elements, focusing on geometric forms, primary colors, and often industrial materials, aiming for simplicity and purity."
            "Naive Art Primitivism" -> "Art created by artists without formal training, characterized by a childlike simplicity, bright colors, and often a disregard for the academic rules of perspective or anatomy. Primitivism often draws inspiration from indigenous or pre-industrial cultures."
            "Pop Art" -> "An art movement that emerged in the 1950s and flourished in the 1960s in America and Britain, drawing inspiration from sources in popular and commercial culture such as advertising, comic books, and mundane cultural objects. It challenged traditional fine art values by often using irony and parody."
            "Realism" -> "An artistic movement that began in France in the 1850s, after the 1848 Revolution. Realists sought to depict subjects as they appeared in everyday life, without idealization or stylization. It aimed to show the truth, no matter how mundane or harsh."
            "Renaissance" -> "A period in European history marking the transition from the Middle Ages to modernity and covering the 15th and 16th centuries. It was characterized by a renewed interest in classical art, literature, and philosophy, leading to masterpieces emphasizing humanism, perspective, and naturalism."
            "Rococo" -> "An 18th-century art style, lighter and more playful than Baroque, characterized by elaborate ornamentation, asymmetrical designs, pastel colors, and themes of love, nature, and social gatherings."
            "Romanticism" -> "An artistic, literary, musical, and intellectual movement that originated in Europe toward the end of the 18th century, and in most areas was at its peak in the approximate period from 1800 to 1850. It emphasized emotion and individualism as well as glorification of all the past and nature, preferring the medieval rather than the classical."
            "Symbolism" -> "Reacts against realism and naturalism, seeking to express abstract ideas, emotions, and spiritual truths through symbolic imagery, mythology, and dreamlike qualities."
            "Ukiyo-e" -> "A genre of Japanese art from the 17th to 19th centuries, typically depicting beautiful women, kabuki actors, sumo wrestlers, scenes from history and folk tales, travel scenes, and landscapes, often produced as woodblock prints and paintings."
            else -> "No detailed description available for this art style."
        }
    }

    /**
     * Returns the drawable resource ID for a given art style.
     * Ensure you have corresponding images in `res/drawable/` with names
     * like `art_nouveau_modern.jpg`, `baroque.jpg`, etc.
     */
    fun getStyleImageResId(artStyle: String?): Int {
        return when (artStyle) {
            "Art Nouveau Modern" -> R.drawable.art_nouveau_modern
            "Baroque" -> R.drawable.baroque
            "Color Field Painting" -> R.drawable.color_field_painting
            "Cubism" -> R.drawable.cubism
            "Expressionism" -> R.drawable.expressionism
            "Fauvism" -> R.drawable.fauvism
            "Impressionism" -> R.drawable.impressionism
            "Minimalism" -> R.drawable.minimalism
            "Naive Art Primitivism" -> R.drawable.naive_art_primitivism
            "Pop Art" -> R.drawable.pop_art
            "Realism" -> R.drawable.realism
            "Renaissance" -> R.drawable.renaissance
            "Rococo" -> R.drawable.rococo
            "Romanticism" -> R.drawable.romanticism
            "Symbolism" -> R.drawable.symbolism
            "Ukiyo-e" -> R.drawable.ukiyo_e
            else -> R.drawable.default_art_style_image // IMPORTANT: Create this drawable!
        }
    }
}

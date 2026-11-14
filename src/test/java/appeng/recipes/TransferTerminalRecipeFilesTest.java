package appeng.recipes;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/**
 * GTNH loads AE2's recipes from {@code GTNHRecipes/} instead of {@code recipes/}, see {@code RecipeLoader}. The test
 * server only loads {@code recipes/}, so these follow each folder's imports the way the recipe loader does and check
 * the Transfer Terminal can be crafted either way.
 */
public class TransferTerminalRecipeFilesTest {

    private static final String RECIPE = "shapeless= ae2:ItemPart.InterfaceTerminal ae2:ItemPart.StorageBus"
            + " ae2:ItemMaterial.LogicProcessor -> ae2:ItemPart.TransferTerminal";

    @Test
    public void theGtnhRecipesCraftTheTransferTerminal() throws IOException {
        assertReaches("/assets/appliedenergistics2/GTNHRecipes/");
    }

    @Test
    public void theDefaultRecipesCraftTheTransferTerminal() throws IOException {
        assertReaches("/assets/appliedenergistics2/recipes/");
    }

    private static void assertReaches(final String root) throws IOException {
        final StringBuilder recipes = new StringBuilder();
        read(root, "index.recipe", recipes, new HashSet<>());
        assertTrue(
                "The recipes imported from " + root + "index.recipe should craft the Transfer Terminal",
                recipes.toString().contains(RECIPE));
    }

    /**
     * Appends the file and everything it imports, with comments dropped and whitespace collapsed to single spaces.
     */
    private static void read(final String root, final String path, final StringBuilder out, final Set<String> seen)
            throws IOException {
        if (!seen.add(path)) {
            return;
        }

        final InputStream stream = TransferTerminalRecipeFilesTest.class.getResourceAsStream(root + path);
        assertNotNull("Imported recipe file " + root + path + " should exist", stream);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                final int comment = line.indexOf('#');
                final String text = (comment < 0 ? line : line.substring(0, comment)).trim();
                if (text.startsWith("import=")) {
                    read(root, text.substring("import=".length()).trim(), out, seen);
                } else if (!text.isEmpty()) {
                    out.append(text.replaceAll("\\s+", " ")).append(' ');
                }
            }
        }
    }
}

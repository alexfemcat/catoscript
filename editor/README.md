# catoscript editor support

VS Code syntax highlighting for `.cato` files, using the palette from
Kernel Panic's CatoDE editor (cyan / pink / yellow / purple / green / orange
on dark).

## Install (local, no marketplace needed)

```bash
cd editor
npx --yes vsce package
code --install-extension catoscript-0.1.0.vsix
```

Restart VS Code. Open any `.cato` file. The grammar lights up:
keywords by category, strings yellow, variables red, labels bright red,
comments slate italic.

## Files

- `syntaxes/catoscript.tmLanguage.json` — the TextMate grammar (which
  token is which)
- `themes/catoscript-color-theme.json` — the color palette (which
  token gets which color)
- `catoscript.language-configuration.json` — auto-close `"..."` pairs
- `package.json` — VS Code extension manifest

## Source

Keyword list comes verbatim from `cato/data/catoKeywords.ts` in
`KernelPanic-and-CatoScript-DE-Master`. Color values come from
`cato/components/Editor.tsx` (the Prism token classes there). Both are
the upstream source of truth; this folder just adapts them for VS Code.

When new keywords land in KP's `catoKeywords.ts`, copy them into
`syntaxes/catoscript.tmLanguage.json` under the right category, then
repackage.
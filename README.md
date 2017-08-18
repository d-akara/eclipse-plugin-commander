# Eclipse Commander
Provides command palette [KAVI](https://github.com/dakaraphi/kavi) interfaces exceeding capabilities of typical sublime like command palettes

Current interfaces provided:
 * **Commander** - provides interface for selecting and executing all Eclipse commands.
 * **Finder** - provides interface for selecting and opening workspace resources.
 
# KAVI implemented features for all interfaces 
 
## Term Matching

### Multi column
Default match will search across columns for matches.
![multi column](/readme-images/multi-column-match.gif)
### Specific column
Columns can be selected individual for matches by using a `,` to separate the column filters.
![specific column](/readme-images/specific-column-match.gif)
### Literal 
A space preceding filter text will cause the following text to be matched literal instead of fuzzy.
![literal column](/readme-images/literal-match.gif) 
### Fuzzy multi word out of order
Contiguous characters matched using a fuzzy strategy that attempts to match words in any order.
A space separating words will force matching of the literal words also allowing for out of order matces.
![fuzzy column](/readme-images/fuzzy-out-of-order-match.gif)
### Quality filtering
### Acronym
Fuzzy matching also will attempt to match by acronym
![acronym column](/readme-images/acronym-match.gif)
## Ranking Sort

## Fast Select
### Immediate action invocation
### Multi select

## Selections
### Single select
### Range select
### Inverse select
### All select/deselect
### Implied selections

## Navigation
### List paging

## Working
### Recent
### Favorites
### Export/Import preferences
### Across workspaces

## Context Actions
### Favorites
### Sort

## Modes and selection states

# Commander
## Columns
## Launcher
# Finder
## Columns
##

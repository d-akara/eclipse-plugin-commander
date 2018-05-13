## 1.3.0 - 5.13.2018
### Features
- New Settings Preference: Auto close dialog on focus lost
    - When set, the dialog will now auto close when clicking outside the dialog
- New Settings Command: Favorites Unset Selected As Favorite
    - Remove `Favorite` attribute from item in `working`, but leave item in `working` view
- New Settings Command: Favorites Clear All Except Favorites From Working View
    - Removes all items in `working` excpet those marked as favorites
- Improved color distinction the `Favorite` marker to be more visible
## 1.2.0 - 3.20.2018
### Features
- Finder dialog launch speed significantly improved for extremely large projects
- Settings and history can now be directly exported and imported within Commander and Finder 
    - Finder settings and history and now stored per workspace
    - Due to per workspace storage change, previous Finder history will not be kept on update to this version
- Default mode `working` or `discovery` can now be set as a setting
### Fixes
- Possibly NPE in Finder that prevented launch of dialog
## 1.1.1 - 11.13.2017
### Features
- Types `Eclipse Open Type` have been added to Finder
    - Note, Finder only include types with source attached for now
- Matches can now be excluded using `!` followed by text to exclude
- Copy to clipboard action now copies all columns to the clipboard
- Improved acronym style matching
- Debounce heuristics for Finder improved to be more responsive to allow for 100k+ types for large projects
### Fixes
- Literal matching broken when matching multiple columns
- Context action `;` invocation not working on some non US keyboards
## 1.0.9 - 09.16.2017
First public release

# Responsive navigation

The main app uses a leading navigation rail when the window is at least 600dp
wide and 440dp high. Smaller windows use the floating bottom dock. Both layouts
provide access to Timetable, Calendar, Notes, and Tasks; the rail also offers
shortcuts to Vacant Rooms and Settings.

## Layout behavior

The rail scrolls when its contents exceed the available height, including when
the system font size increases. Tab content has one composition call site across
dock and rail layouts to preserve screen state during resizing.

`Chrome` supplies the active navigation clearance. All four tabs use it for list
bottom padding and FAB placement, so wide windows do not reserve space for an
absent bottom dock. The phone dock is hidden while editing a note or displaying
the keyboard, preserving the existing phone behavior.

## Validation

The existing Android CI performs Kotlin compilation, debug/release assembly,
signing verification, unit tests, and database asset checks. Local checks cover
diff whitespace, the prepopulated database validator, and the changed-file list.
Android compilation runs in CI because the local VM is resource constrained.

Device visual checks cover phone dock navigation, tablet rail navigation and
shortcuts, resizing between modes, small landscape windows, large font sizes,
keyboard visibility, and list/FAB clearance on all four tabs. These checks remain
outstanding until exercised on a device or emulator.

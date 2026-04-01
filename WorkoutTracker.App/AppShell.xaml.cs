namespace WorkoutTracker.App;

public partial class AppShell : Shell
{
	public AppShell()
	{
		InitializeComponent();
        Routing.RegisterRoute(nameof(Pages.WorkoutEditorPage), typeof(Pages.WorkoutEditorPage));
        Routing.RegisterRoute(nameof(Pages.WorkoutDetailPage), typeof(Pages.WorkoutDetailPage));
        Routing.RegisterRoute(nameof(Pages.SettingsPage), typeof(Pages.SettingsPage));
	}
}

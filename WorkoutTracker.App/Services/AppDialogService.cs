namespace WorkoutTracker.App.Services;

public interface IAppDialogService
{
    Task<bool> ConfirmAsync(string title, string message, string accept = "Confirm", string cancel = "Cancel");
}

public sealed class AppDialogService : IAppDialogService
{
    public Task<bool> ConfirmAsync(string title, string message, string accept = "Confirm", string cancel = "Cancel") =>
        MainThread.InvokeOnMainThreadAsync(async () =>
        {
            var page = Shell.Current?.CurrentPage
                ?? Application.Current?.Windows.FirstOrDefault()?.Page;

            if (page is null)
            {
                return false;
            }

            return await page.DisplayAlert(title, message, accept, cancel);
        });
}

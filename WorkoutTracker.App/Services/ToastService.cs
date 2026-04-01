namespace WorkoutTracker.App.Services;

public interface IToastService
{
    Task ShowAsync(string message);
}

public sealed class ToastService : IToastService
{
    public Task ShowAsync(string message)
    {
        if (string.IsNullOrWhiteSpace(message))
        {
            return Task.CompletedTask;
        }

        return MainThread.InvokeOnMainThreadAsync(async () =>
        {
#if ANDROID
            var context = Android.App.Application.Context;
            if (Android.Widget.Toast.MakeText(context, message, Android.Widget.ToastLength.Short) is { } toast)
            {
                toast.SetGravity(Android.Views.GravityFlags.Top | Android.Views.GravityFlags.CenterHorizontal, 0, 180);
                toast.Show();
            }
#else
            var page = Shell.Current?.CurrentPage ?? Application.Current?.Windows.FirstOrDefault()?.Page;
            if (page is not null)
            {
                await page.DisplayAlert("Notice", message, "OK");
            }
#endif
        });
    }
}

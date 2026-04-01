using CommunityToolkit.Mvvm.ComponentModel;

namespace WorkoutTracker.App.ViewModels;

public abstract partial class BaseViewModel : ObservableObject
{
    [ObservableProperty]
    private bool isBusy;

    [ObservableProperty]
    private string title = string.Empty;

    [ObservableProperty]
    private string statusMessage = string.Empty;

    protected async Task RunBusyAsync(Func<Task> action, string? successMessage = null)
    {
        if (IsBusy)
        {
            return;
        }

        try
        {
            IsBusy = true;
            StatusMessage = string.Empty;
            await action().ConfigureAwait(false);
            if (!string.IsNullOrWhiteSpace(successMessage))
            {
                StatusMessage = successMessage;
                await ShowToastAsync(successMessage).ConfigureAwait(false);
            }
        }
        catch (Exception ex)
        {
            StatusMessage = ex.Message;
            await ShowToastAsync(ex.Message).ConfigureAwait(false);
        }
        finally
        {
            IsBusy = false;
        }
    }

    private static Task ShowToastAsync(string message)
    {
        try
        {
            return ServiceHelper.GetService<Services.IToastService>().ShowAsync(message);
        }
        catch
        {
            return Task.CompletedTask;
        }
    }
}

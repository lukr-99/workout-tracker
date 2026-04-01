using WorkoutTracker.App.ViewModels;

namespace WorkoutTracker.App.Pages;

public partial class WorkoutDetailPage : ContentPage, IQueryAttributable
{
    private readonly WorkoutDetailViewModel _viewModel;

    public WorkoutDetailPage()
    {
        InitializeComponent();
        _viewModel = ServiceHelper.GetService<WorkoutDetailViewModel>();
        BindingContext = _viewModel;
    }

    public async void ApplyQueryAttributes(IDictionary<string, object> query)
    {
        if (query.TryGetValue("sessionId", out var raw) && raw is string sessionId)
        {
            await _viewModel.LoadAsync(sessionId);
        }
    }
}
